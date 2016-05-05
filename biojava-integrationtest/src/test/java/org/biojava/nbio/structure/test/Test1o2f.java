/*
 *                    BioJava development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the individual
 * authors.  These should be listed in @author doc comments.
 *
 * For more information on the BioJava project and its aims,
 * or to join the biojava-l mailing list, visit the home page
 * at:
 *
 *      http://www.biojava.org/
 *
 */
package org.biojava.nbio.structure.test;

import org.biojava.nbio.structure.Chain;
import org.biojava.nbio.structure.Structure;
import org.biojava.nbio.structure.StructureIO;
import org.biojava.nbio.structure.align.util.AtomCache;
import org.biojava.nbio.structure.io.FileParsingParameters;
import org.biojava.nbio.structure.io.LocalPDBDirectory.FetchBehavior;

import junit.framework.TestCase;

public class Test1o2f extends TestCase{

	private static Structure structure = null;


	@Override
	protected void setUp() throws Exception {
		super.setUp();
		AtomCache cache = new AtomCache();
		cache.setUseMmCif(true);
		cache.setFetchBehavior(FetchBehavior.FETCH_FILES);
		StructureIO.setAtomCache(cache);
		String pdbId = "1O2F";
		structure = StructureIO.getStructure(pdbId);
	}


	public void test1a4wPDBFile(){
		for(int i=0;i<structure.nrModels();i++){
			for(Chain c: structure.getChains(i)){
				assertNotNull(c.getChainID());
				assertNotNull(c.getInternalChainID());
			}
		}
	}
}
