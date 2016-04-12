package org.biojava.nbio.structure.io.mmtf;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.biojava.nbio.structure.Atom;
import org.biojava.nbio.structure.Bond;
import org.biojava.nbio.structure.Chain;
import org.biojava.nbio.structure.EntityInfo;
import org.biojava.nbio.structure.Group;
import org.biojava.nbio.structure.PDBCrystallographicInfo;
import org.biojava.nbio.structure.PDBHeader;
import org.biojava.nbio.structure.Structure;
import org.biojava.nbio.structure.io.mmcif.model.ChemComp;
import org.biojava.nbio.structure.quaternary.BioAssemblyInfo;
import org.rcsb.mmtf.api.MmtfDecoderInterface;
import org.rcsb.mmtf.api.MmtfWriter;
import org.rcsb.mmtf.dataholders.MmtfBean;

/**
 * Class to take Biojava structure data and covert to the DataApi for encoding. 
 * Must implement all the functions in {@link MmtfDecoderInterface}.
 * @author Anthony Bradley
 *
 */
public class MmtfStructureWriter implements MmtfWriter {


	private MmtfDecoderInterface mmtfDecoderInterface;
	private Structure structure;


	/**
	 * The constructor requires a structre input.
	 * @param mmtfDecoderInterface the interface to be used
	 * @param structure the structure to be encoded
	 */
	public MmtfStructureWriter(Structure data) {
		this.structure = data;
	}

	public void write(MmtfDecoderInterface decoder) {
		this.mmtfDecoderInterface = decoder;
		// Reset structure to consider altloc groups with the same residue number but different group names as seperate groups
		MmtfUtils.fixMicroheterogenity(structure);
		// Generate the secondary structure
		MmtfUtils.calculateDsspSecondaryStructure(structure);
		// Get the chain name to index map
		Map<String, Integer> chainIdToIndexMap = MmtfUtils.getChainIdToIndexMap(structure);
		List<Chain> allChains = MmtfUtils.getAllChains(structure);
		List<Atom> allAtoms = MmtfUtils.getAllAtoms(structure);
		int numBonds = MmtfUtils.getNumBonds(allAtoms);
		mmtfDecoderInterface.initStructure(numBonds, allAtoms.size(), MmtfUtils.getNumGroups(structure), allChains.size(), structure.nrModels(), structure.getPDBCode());
		// Get the header and the xtal info.
		PDBHeader pdbHeader = structure.getPDBHeader();
		PDBCrystallographicInfo xtalInfo = pdbHeader.getCrystallographicInfo();
		mmtfDecoderInterface.setHeaderInfo(pdbHeader.getRfree(), 1.0f, pdbHeader.getResolution(), pdbHeader.getTitle(), MmtfUtils.dateToIsoString(pdbHeader.getDepDate()), 
				MmtfUtils.techniquesToStringArray(pdbHeader.getExperimentalTechniques()));
		mmtfDecoderInterface.setXtalInfo(MmtfUtils.getSpaceGroupAsString(xtalInfo.getSpaceGroup()), MmtfUtils.getUnitCellAsArray(xtalInfo));
		// Store the bioassembly data
		storeBioassemblyInformation(chainIdToIndexMap, pdbHeader.getBioAssemblies());
		// Store the entity data
		storeEntityInformation(allChains, structure.getEntityInfos());
		// Now loop through the data structure
		for (int modelIndex=0; modelIndex<structure.nrModels(); modelIndex++) {
			List<Chain> modelChains = structure.getChains(modelIndex);
			// Set this model
			mmtfDecoderInterface.setModelInfo(modelIndex, modelChains.size());
			for(int chainInModelIndex=0; chainInModelIndex<modelChains.size(); chainInModelIndex++) {
				Chain chain = modelChains.get(chainInModelIndex);
				List<Group> groups = chain.getAtomGroups();
				List<Group> sequenceGroups = chain.getSeqResGroups();
				mmtfDecoderInterface.setChainInfo(chain.getChainID(), chain.getInternalChainID(), groups.size());
				for(int groupInChainIndex=0; groupInChainIndex<groups.size(); groupInChainIndex++){
					// Get the major compy of this group
					Group group = groups.get(groupInChainIndex);
					List<Atom> atomsInGroup = MmtfUtils.getAtomsForGroup(group);
					// Get the group type
					ChemComp chemComp = group.getChemComp();
					Character insCode = group.getResidueNumber().getInsCode();
					if(insCode==null){
						insCode=MmtfBean.UNAVAILABLE_CHAR_VALUE;
					}
					mmtfDecoderInterface.setGroupInfo(group.getPDBName(), group.getResidueNumber().getSeqNum(), insCode.charValue(), 
							chemComp.getType(), atomsInGroup.size(), MmtfUtils.getNumBondsInGroup(atomsInGroup), chemComp.getOne_letter_code().charAt(0),
							sequenceGroups.indexOf(group), MmtfUtils.getSecStructType(group));
					for (Atom atom : atomsInGroup){
						char altLoc = MmtfBean.UNAVAILABLE_CHAR_VALUE;
						if(atom.getAltLoc()!=null){
							altLoc=atom.getAltLoc().charValue();
						}
						mmtfDecoderInterface.setAtomInfo(atom.getName(), atom.getPDBserial(), altLoc, (float) atom.getX(), 
								(float) atom.getY(), (float) atom.getZ(), atom.getOccupancy(), 
								atom.getTempFactor(), atom.getElement().toString(), atom.getCharge());
						addBonds(atom, atomsInGroup, allAtoms);
					}
				}
			}
		}
		mmtfDecoderInterface.finalizeStructure();

	}

	/**
	 * Add the bonds for a given atom.
	 * @param atom the atom for which bonds are to be formed
	 * @param atomsInGroup the list of atoms in the group
	 * @param allAtoms the list of atoms in the whole structure
	 */
	private void addBonds(Atom atom, List<Atom> atomsInGroup, List<Atom> allAtoms) {
		if(atom.getBonds()==null){
			return;
		}
		for(Bond bond : atom.getBonds()) {
			// Now set the bonding information.
			Atom other = bond.getOther(atom);
			// If both atoms are in the group
			if (atomsInGroup.indexOf(other)!=-1){
				Integer firstBondIndex = atomsInGroup.indexOf(atom);
				Integer secondBondIndex = atomsInGroup.indexOf(other);
				// Don't add the same bond twice
				if(firstBondIndex>secondBondIndex){
					int bondOrder = bond.getBondOrder();
					mmtfDecoderInterface.setGroupBond(firstBondIndex, secondBondIndex, bondOrder);
				}
			}
			// Otherwise it's an inter group bond - so add it here
			else {
				Integer firstBondIndex = allAtoms.indexOf(atom);
				Integer secondBondIndex = allAtoms.indexOf(other);
				if(firstBondIndex<secondBondIndex){
					// Don't add the same bond twice
					int bondOrder = bond.getBondOrder();							
					mmtfDecoderInterface.setInterGroupBond(firstBondIndex, secondBondIndex, bondOrder);
				}
			}
		}		
	}


	/**
	 * Store the entity information for a given structure.
	 * @param allChains a list of all the chains in a structure
	 * @param entityInfos a list of the entity information
	 */
	private void storeEntityInformation(List<Chain> allChains, List<EntityInfo> entityInfos) {
		for (EntityInfo entityInfo : entityInfos) {
			String description = entityInfo.getDescription();
			String details = entityInfo.getDetails();
			List<Chain> entityChains = entityInfo.getChains();
			int[] chainIndices = new int[entityChains.size()];
			for (int i=0; i<entityChains.size(); i++) {
				chainIndices[i] = allChains.indexOf(entityChains.get(i));
			}
			String sequence = entityChains.get(0).getSeqResSequence();
			mmtfDecoderInterface.setEntityInfo(chainIndices, sequence, description, details);
		}		
	}


	/**
	 * Generate the bioassembly information on in the desired form.
	 * @param bioJavaStruct the Biojava structure
	 * @param header the header
	 */
	private void storeBioassemblyInformation(Map<String, Integer> chainIdToIndexMap, Map<Integer, BioAssemblyInfo> inputBioAss) {
		int bioAssemblyIndex = 0;
		for (Entry<Integer, BioAssemblyInfo> entry : inputBioAss.entrySet()) {
			Map<double[], int[]> transformMap = MmtfUtils.getTransformMap(entry.getValue(), chainIdToIndexMap);
			for(Entry<double[], int[]> transformEntry : transformMap.entrySet()) {
				mmtfDecoderInterface.setBioAssemblyTrans(bioAssemblyIndex, transformEntry.getValue(), transformEntry.getKey());
			}
			 bioAssemblyIndex+=1;
		}
	}

}
