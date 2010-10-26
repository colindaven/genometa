package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.symmetry.SingletonSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.MutableSeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;


public class SeqUtilsTest {
	BioSeq seqA;
	BioSeq seqB;

	public SeqUtilsTest() {
	}

	@Before
		public void setUp() throws Exception {
			seqA = new BioSeq("Seq A", "version", 1000000);
			seqB = new BioSeq("Seq B", "version", 1000000);
		}

	@After
		public void tearDown() throws Exception {
		}

	@Test
	public void testDepth() {
		BioSeq seq = seqA;
		SeqSymmetry symA;
		SeqSymmetry symB;
		MutableSeqSymmetry result;

		symA = new SingletonSeqSymmetry(300, 400, seq);
		symB = new SingletonSeqSymmetry(700, 900, seq);

		int depth = SeqUtils.getDepth(symA);
		assertEquals(1, depth);
     
		result = SeqUtils.union(symA, symB, seq);
		depth = SeqUtils.getDepth(result);
		assertEquals(2, depth);
		
	}

	@Test
		public void testUnion() {
			BioSeq seq = seqA;
			SeqSymmetry symA;
			SeqSymmetry symB;
			MutableSeqSymmetry result;
			SeqSpan result_span;

			symA = new SingletonSeqSymmetry(300, 400, seq);
			symB = new SingletonSeqSymmetry(700, 900, seq);
			result = SeqUtils.union(symA, symB, seq);
			result_span = result.getSpan(seq);
			assertEquals(300, result_span.getStart());
			assertEquals(900, result_span.getEnd());
			assertTrue(result != symA);
			assertTrue(result != symB);

			symA = new SingletonSeqSymmetry(300, 400, seq);
			symB = new SingletonSeqSymmetry(1000, 600, seq);
			result = SeqUtils.union(symA, symB, seq);
			result_span = result.getSpan(seq);
			assertEquals(300, result_span.getStart());
			assertEquals(1000, result_span.getEnd());

			symA = new SingletonSeqSymmetry(400, 300, seq);
			symB = new SingletonSeqSymmetry(700, 900, seq);
			result = SeqUtils.union(symA, symB, seq);
			result_span = result.getSpan(seq);
			assertEquals(300, result_span.getStart());
			assertEquals(900, result_span.getEnd());

			// The union symmetry is always oriented in the '+' direction.
			symA = new SingletonSeqSymmetry(400, 300, seq);
			symB = new SingletonSeqSymmetry(1000, 200, seq);
			result = SeqUtils.union(symA, symB, seq);
			result_span = result.getSpan(seq);
			assertEquals(200, result_span.getStart());
			assertEquals(1000, result_span.getEnd());

			MutableSeqSymmetry symC = new SimpleMutableSeqSymmetry();
			symC.addChild(new SingletonSeqSymmetry(100, 200, seqA));
			symC.addChild(new SingletonSeqSymmetry(500, 600, seqA));
			symC.addChild(new SingletonSeqSymmetry(1000, 1100, seqA));

			MutableSeqSymmetry symD = new SimpleMutableSeqSymmetry();
			symD.addChild(new SingletonSeqSymmetry(900, 800, seqA));
			symD.addChild(new SingletonSeqSymmetry(600, 500, seqA));
			symD.addChild(new SingletonSeqSymmetry(1000, 1200, seqB));
			symD.addChild(new SingletonSeqSymmetry(300, 150, seqA));

			result = SeqUtils.union(symC, symD, seqA);
			assertNotNull(result);
			//SeqUtils.printSymmetry(result);

			assertEquals(4, result.getChildCount());
			assertEquals(100, result.getSpan(seqA).getStart());
			assertEquals(1100, result.getSpan(seqA).getEnd());
			assertEquals(100, result.getSpan(seqA).getMin());
			assertEquals(1100, result.getSpan(seqA).getMax());
			assertTrue(result.getSpan(seqA).isForward());

			assertTrue(result.getChild(0).getSpan(seqA).isForward());
			assertEquals(100, result.getChild(0).getSpan(seqA).getStart());
			assertEquals(300, result.getChild(0).getSpan(seqA).getEnd());

			assertTrue(result.getChild(1).getSpan(seqA).isForward());
			assertEquals(500, result.getChild(1).getSpan(seqA).getStart());
			assertEquals(600, result.getChild(1).getSpan(seqA).getEnd());

			assertFalse(result.getChild(2).getSpan(seqA).isForward());

			assertEquals(900, result.getChild(2).getSpan(seqA).getStart());
			assertEquals(800, result.getChild(2).getSpan(seqA).getEnd());

			assertEquals(900, result.getChild(2).getSpan(seqA).getMax());
			assertEquals(800, result.getChild(2).getSpan(seqA).getMin());



			assertEquals(1000, result.getChild(3).getSpan(seqA).getStart());
			assertEquals(1100, result.getChild(3).getSpan(seqA).getEnd());
		}

	@Test
	  public void testIntersection() {
	    BioSeq seq = seqA;
			SeqSymmetry symA;
			SeqSymmetry symB;
			MutableSeqSymmetry result;
			SeqSpan result_span;

			symA = new SingletonSeqSymmetry(300, 400, seq);
			symB = new SingletonSeqSymmetry(700, 900, seq);
			result = SeqUtils.intersection(symA, symB, seq);
			result_span = result.getSpan(seq);
			assertNull(result_span);

			symA = new SingletonSeqSymmetry(300, 400, seq);
			symB = new SingletonSeqSymmetry(1000, 600, seq);
			result = SeqUtils.intersection(symA, symB, seq);
			result_span = result.getSpan(seq);
			assertNull(result_span);

			symA = new SingletonSeqSymmetry(400, 300, seq);
			symB = new SingletonSeqSymmetry(700, 900, seq);
			result = SeqUtils.intersection(symA, symB, seq);
			result_span = result.getSpan(seq);
			assertNull(result_span);

			// The intersection symmetry is always oriented in the '+' direction.
			symA = new SingletonSeqSymmetry(400, 300, seq);
			symB = new SingletonSeqSymmetry(1000, 200, seq);
			result = SeqUtils.intersection(symA, symB, seq);
			result_span = result.getSpan(seq);
			assertEquals(300, result_span.getStart());
			assertEquals(400, result_span.getEnd());

			MutableSeqSymmetry symC = new SimpleMutableSeqSymmetry();
			symC.addChild(new SingletonSeqSymmetry(100, 200, seqA));
			symC.addChild(new SingletonSeqSymmetry(500, 600, seqA));
			symC.addChild(new SingletonSeqSymmetry(1000, 1100, seqA));

			MutableSeqSymmetry symD = new SimpleMutableSeqSymmetry();
			symD.addChild(new SingletonSeqSymmetry(900, 800, seqA));
			symD.addChild(new SingletonSeqSymmetry(600, 500, seqA));
			symD.addChild(new SingletonSeqSymmetry(1000, 1200, seqB));
			symD.addChild(new SingletonSeqSymmetry(300, 150, seqA));

			result = SeqUtils.intersection(symC, symD, seqA);
			assertNotNull(result);

			// where is 1000-1100?
			assertEquals(2, result.getChildCount());
			assertEquals(150, result.getSpan(seqA).getStart());
			assertEquals(600, result.getSpan(seqA).getEnd());
			assertEquals(150, result.getSpan(seqA).getMin());
			assertEquals(600, result.getSpan(seqA).getMax());
			assertTrue(result.getSpan(seqA).isForward());

			assertTrue(result.getChild(0).getSpan(seqA).isForward());
			assertEquals(150, result.getChild(0).getSpan(seqA).getStart());
			assertEquals(200, result.getChild(0).getSpan(seqA).getEnd());

			assertTrue(result.getChild(1).getSpan(seqA).isForward());
			assertEquals(500, result.getChild(1).getSpan(seqA).getStart());
			assertEquals(600, result.getChild(1).getSpan(seqA).getEnd());
		

			result = SeqUtils.intersection(symD, symC, seqA);
			assertNotNull(result);

			assertEquals(2, result.getChildCount());
			assertEquals(150, result.getSpan(seqA).getStart());
			assertEquals(600, result.getSpan(seqA).getEnd());
			assertEquals(150, result.getSpan(seqA).getMin());
			assertEquals(600, result.getSpan(seqA).getMax());
			assertTrue(result.getSpan(seqA).isForward());

			assertFalse(result.getChild(0).getSpan(seqA).isForward());
			assertEquals(200, result.getChild(0).getSpan(seqA).getStart());
			assertEquals(150, result.getChild(0).getSpan(seqA).getEnd());

			assertFalse(result.getChild(1).getSpan(seqA).isForward());
			assertEquals(600, result.getChild(1).getSpan(seqA).getStart());
			assertEquals(500, result.getChild(1).getSpan(seqA).getEnd());

	}
	//  
	//  public void testOverlap() {
	//    System.out.println("overlap");
	//    
	//    SeqSpan spanA = null;
	//    SeqSpan spanB = null;
	//    
	//    boolean expResult = true;
	//    boolean result = SeqUtils.overlap(spanA, spanB);
	//    assertEquals(expResult, result);
	//    
	//    fail("The test case is a prototype.");
	//  }
	//  
	//  public void testLooseOverlap() {
	//    System.out.println("looseOverlap");
	//    
	//    SeqSpan spanA = null;
	//    SeqSpan spanB = null;
	//    
	//    boolean expResult = true;
	//    boolean result = SeqUtils.looseOverlap(spanA, spanB);
	//    assertEquals(expResult, result);
	//    
	//    fail("The test case is a prototype.");
	//  }
	//  
	//  public void testStrictOverlap() {
	//    System.out.println("strictOverlap");
	//    
	//    SeqSpan spanA = null;
	//    SeqSpan spanB = null;
	//    
	//    boolean expResult = true;
	//    boolean result = SeqUtils.strictOverlap(spanA, spanB);
	//    assertEquals(expResult, result);
	//    
	//    fail("The test case is a prototype.");
	//  }
	//  
	//  public void testIntersects() {
	//    System.out.println("intersects");
	//    
	//    SeqSpan spanA = null;
	//    SeqSpan spanB = null;
	//    
	//    boolean expResult = true;
	//    boolean result = SeqUtils.intersects(spanA, spanB);
	//    assertEquals(expResult, result);
	//    
	//    fail("The test case is a prototype.");
	//  }
	//  
	//  public void testEncompass() {
	//    System.out.println("encompass");
	//    
	//    SeqSpan spanA = null;
	//    SeqSpan spanB = null;
	//    
	//    SeqSpan expResult = null;
	//    SeqSpan result = SeqUtils.encompass(spanA, spanB);
	//    assertEquals(expResult, result);
	//    
	//    fail("The test case is a prototype.");
	//  }

	/**
	 * Test of transformSymmetry method, of class com.affymetrix.genometry.util.SeqUtils.
	 */
	@Test
		public void testTransformSymmetry() {
			BioSeq annot_seq = new BioSeq("annot", "version", 1000000);
			BioSeq view_seq = new BioSeq("view_seq", "version", 1000000);

			// First create a slicing transforming symmetry such that
			// region from 500-600 in seqA get transformed to region 0-100 in seqB
			// etc., as in the arrays starts[] and stops[]

			int startsA[] = new int[] {500,  900, 1100, 1300, 1500, 1900};
			int stopsA[] = new int[]  {600, 1000, 1200, 1400, 1600, 2100};

			int startsV[] = new int[] {0,    100,  200,  300,  400,  500};
			int stopsV[] = new int[]  {100,  200,  300,  400,  500,  700};

			MutableSeqSymmetry transformer = new SimpleMutableSeqSymmetry();
			// These next two lines are necessary.  But should they be?
			transformer.addSpan(new SimpleMutableSeqSpan(startsA[0], stopsA[stopsA.length-1], annot_seq));
			transformer.addSpan(new SimpleMutableSeqSpan(startsV[0], stopsV[stopsV.length-1], view_seq));

			for (int i=0; i<startsA.length; i++) {
				MutableSeqSymmetry child = new SimpleMutableSeqSymmetry();
				child.addSpan(new SimpleMutableSeqSpan(startsA[i], stopsA[i], annot_seq));
				child.addSpan(new SimpleMutableSeqSpan(startsV[i], stopsV[i], view_seq));
				transformer.addChild(child);
			}

			// Now create a symmetry to be transformed.
			// This one has many children, all of length 50, with starts from the array starts[]

			int starts[] = new int[] {125, 325, 525, 725, 925, 1125, 1225, 1525, 1725, 1925, 2125};

			MutableSeqSymmetry initialSym = new SimpleMutableSeqSymmetry();
			for (int i=0; i<starts.length; i++) {
				MutableSeqSymmetry child = new SimpleMutableSeqSymmetry();
				child.addSpan(new SimpleMutableSeqSpan(starts[i], starts[i] + 50, annot_seq));
				initialSym.addChild(child);
			}

			// Now do the transformation

			boolean result = SeqUtils.transformSymmetry(initialSym, transformer, true);
			assertEquals(true, result);

			assertEquals(11, initialSym.getChildCount());
			assertNull(initialSym.getChild(0).getSpan(view_seq));
			assertNull(initialSym.getChild(1).getSpan(view_seq));
			assertNotNull(initialSym.getChild(2).getSpan(view_seq));
			assertNull(initialSym.getChild(3).getSpan(view_seq));
			assertNotNull(initialSym.getChild(4).getSpan(view_seq));
			assertNotNull(initialSym.getChild(5).getSpan(view_seq));
			assertNull(initialSym.getChild(6).getSpan(view_seq));
			assertNotNull(initialSym.getChild(7).getSpan(view_seq));
			assertNull(initialSym.getChild(8).getSpan(view_seq));
			assertNotNull(initialSym.getChild(9).getSpan(view_seq));
			assertNull(initialSym.getChild(10).getSpan(view_seq));

			assertEquals(2050, initialSym.getSpan(annot_seq).getLength());
			assertEquals(550, initialSym.getSpan(view_seq).getLength());

			assertEquals(25, initialSym.getChild(2).getSpan(view_seq).getStart());
			assertEquals(75, initialSym.getChild(2).getSpan(view_seq).getEnd());
			assertEquals(125, initialSym.getChild(4).getSpan(view_seq).getStart());
			assertEquals(175, initialSym.getChild(4).getSpan(view_seq).getEnd());
			assertEquals(225, initialSym.getChild(5).getSpan(view_seq).getStart());
			assertEquals(275, initialSym.getChild(5).getSpan(view_seq).getEnd());
			assertEquals(425, initialSym.getChild(7).getSpan(view_seq).getStart());
			assertEquals(475, initialSym.getChild(7).getSpan(view_seq).getEnd());
			assertEquals(525, initialSym.getChild(9).getSpan(view_seq).getStart());
			assertEquals(575, initialSym.getChild(9).getSpan(view_seq).getEnd());

		}
	@Test
	public void testExclusive(){
		SeqSymmetry symA,symB,result;
		SeqSpan result_span;
		BioSeq seq = new BioSeq("seq", "version", 1000);
		symA = new SingletonSeqSymmetry(300, 800, seq);
		symB = new SingletonSeqSymmetry(700, 900, seq);
		result = SeqUtils.exclusive(symA, symB, seq);
		result_span = result.getSpan(seq);
		assertEquals(300, result_span.getStart());
		assertEquals(700, result_span.getEnd());
		assertEquals(400,result_span.getLength());
		
		
	}


	@Test
	public void testTransformSpan(){
		
		BioSeq annot_seq = new BioSeq("annot", "version",1000000);
		SimpleSeqSpan srcSpan = new SimpleSeqSpan(500,800,annot_seq);
		MutableSeqSpan dstSpan = new SimpleMutableSeqSpan(300,900,annot_seq);
        SeqSymmetry sym = new SingletonSeqSymmetry(300, 800, annot_seq);
		boolean result = SeqUtils.transformSpan(srcSpan, dstSpan,annot_seq, sym);
		assertEquals(true, result);
        SeqSymmetry sym2 = new SingletonSeqSymmetry(1000, 2000, annot_seq);
		boolean result2 = SeqUtils.transformSpan(srcSpan, dstSpan,annot_seq, sym2);
		assertEquals(false, result2);
		//Test for same direction spans
		double scale = dstSpan.getLengthDouble() / srcSpan.getLengthDouble();
		assertEquals(1.0d,scale,0.000000);
		SeqSpan span1= sym.getSpan(srcSpan.getBioSeq());
		assertEquals(300,span1.getStart());
        assertEquals(800,span1.getEnd());
		SeqSpan span2 =sym.getSpan(annot_seq);
		assertEquals(300,span2.getStart());
		assertEquals(800,span2.getEnd());
		//Test the vstart and vend for forward strand spans
        double vstart = (scale * (srcSpan.getStartDouble() - span1.getStartDouble())) + span2.getStartDouble();
		double vend = (scale * (srcSpan.getEndDouble() - span1.getEndDouble())) + span2.getEndDouble();
		assertEquals(500.0d,vstart, 0.000001d);
        assertEquals(800.0,vend,0.000000d);
}
	@Test
	public void testGetResidues(){
		BioSeq seq = new BioSeq("seq", "version",30);
		seq.setResidues("atgctgaacaatgactagtaactcgaatga");
		MutableSeqSymmetry sym = new SimpleMutableSeqSymmetry();
		sym.addChild(new SingletonSeqSymmetry(2, 6,seq));
		sym.addChild(new SingletonSeqSymmetry(10,15, seq));
		sym.addChild(new SingletonSeqSymmetry(22, 25, seq));
		String sequence = SeqUtils.getResidues(sym, seq);
		assertNotNull(sequence);
		assertEquals("gctgatgactcg",sequence);

		assertEquals(12,sequence.length());

	}
	@Test
	public void testEncompass(){
		BioSeq annot_seq = new BioSeq("annot", "version",1000000);
		SeqSpan spanA = new SimpleSeqSpan(500,300,annot_seq);
		SeqSpan spanB = new SimpleSeqSpan(800,900,annot_seq);
		MutableSeqSpan dstSpan = new SimpleMutableSeqSpan(300,900,annot_seq);
		boolean result=SeqUtils.encompass(spanA, spanB, dstSpan);
		assertNotNull(result);
		assertTrue(result);
        double resultStart =dstSpan.getStartDouble();
		double resultEnd =dstSpan.getEndDouble();
		//Check for spanA reverse and spanB forward
		assertEquals(900.0d,resultStart,0.000001d);
		assertEquals(300.0d,resultEnd,0.000001d);
		//Check for spanC forward and spanD reverse
		SeqSpan spanC = new SimpleSeqSpan(300,500,annot_seq);
		SeqSpan spanD = new SimpleSeqSpan(900,800,annot_seq);
		MutableSeqSpan dstSpan2 = new SimpleMutableSeqSpan(300,900,annot_seq);
		assertTrue(SeqUtils.encompass(spanC, spanD, dstSpan2));
		double resultStart2 =dstSpan2.getStartDouble();
		double resultEnd2 =dstSpan2.getEndDouble();
		assertEquals(300.0d,resultStart2,0.000001d);
		assertEquals(900.0d,resultEnd2,0.000001d);
		//Check if spanE and spanF are both forward
		SeqSpan spanE = new SimpleSeqSpan(500,1000,annot_seq);
		SeqSpan spanF = new SimpleSeqSpan(800,900,annot_seq);
		MutableSeqSpan dstSpan3 = new SimpleMutableSeqSpan(500,1500,annot_seq);
		assertTrue(SeqUtils.encompass(spanE, spanF, dstSpan3));
		double resultStart3 =dstSpan3.getStartDouble();
		double resultEnd3 =dstSpan3.getEndDouble();
		assertEquals(500.0d,resultStart3,0.000001d);
		assertEquals(1000.0d,resultEnd3,0.000001d);
		//Check if spanG and spanH are both reverse
		SeqSpan spanG = new SimpleSeqSpan(1000,500,annot_seq);
		SeqSpan spanH = new SimpleSeqSpan(900,800,annot_seq);
		MutableSeqSpan dstSpan4 = new SimpleMutableSeqSpan(1500,500,annot_seq);
		assertTrue(SeqUtils.encompass(spanG, spanH, dstSpan4));
		double resultStart4 =dstSpan4.getStartDouble();
		double resultEnd4 =dstSpan4.getEndDouble();
		assertEquals(1000.0d,resultStart4,0.000001d);
		assertEquals(500.0d,resultEnd4,0.000001d);

		//Check for False
		BioSeq annot_seq1 = new BioSeq("annot", "version1",1000000);
		BioSeq annot_seq2 = new BioSeq("annot", "version2",1000000);
        SeqSpan spanI = new SimpleSeqSpan(1000,500,annot_seq1);
		SeqSpan spanJ = new SimpleSeqSpan(900,800,annot_seq2);
		MutableSeqSpan dstSpan5 = new SimpleMutableSeqSpan(1500,500,annot_seq1);
		assertFalse(SeqUtils.encompass(spanI, spanJ, dstSpan5));


	}

	
}
