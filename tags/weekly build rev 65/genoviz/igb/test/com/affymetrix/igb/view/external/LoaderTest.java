package com.affymetrix.igb.view.external;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.junit.Test;


public class LoaderTest extends junit.framework.TestCase {
	private static final String imageDir = "test/com/affymetrix/igb/view/external/images/";

	public void checkDownLoadEnsembl(Loc loc, String name){
		Map<String, String> cookies = new HashMap<String, String>();
		cookies.put(EnsemblView.ENSEMBLSESSION, "");
		cookies.put(EnsemblView.ENSEMBLWIDTH, "800");
		ImageError image = new ENSEMBLoader().getImage(loc, 800, cookies);
	//	saveImage(name, "ensembl", image); //save new images, inspect by eye, comment back
		assertEquals("ensembl "+name,"", image.error);
		compareImage(name, "ensembl",image.image);
	}

	public void checkDownLoadUCSC(Loc loc, String name){
		Map<String, String> cookies = new HashMap<String, String>();
		cookies.put(UCSCView.UCSCUSERID, "");
		ImageError image = new UCSCLoader().getImage(loc, 800, cookies);
	//	saveImage(name, "ucsc", image); //save new images, inspect by eye, comment back
		assertEquals("ucsc "+name,"", image.error);
		compareImage(name, "ucsc",image.image);
	}

	class LocName {
		Loc loc;
		String name;
		LocName(Loc loc, String name){
			this.loc = loc;
			this.name = name;
		}
	}

	public void testRetrieve(){
		List<LocName> locs = Arrays.asList(
				new LocName(new Loc("mm9","chr1",6203693,6206373),  "mouse_mm9.png"),
				new LocName(new Loc("hg19","chr1",6203693,6206373), "human_hg19.png"),
				new LocName(new Loc("dm3","chr2L",6203693,6206373), "drosophila_dm3.png")
		);
		for(LocName ln : locs){
			//checkDownLoadEnsembl(ln.loc,ln.name);
			//checkDownLoadUCSC(ln.loc,ln.name);
		}
	}

	public void saveImage( String name, String type, ImageError imageError){
		try {
			ImageIO.write(imageError.image, "png", new File(imageDir+type+"_"+name));
		} catch (IOException ex) {
			Logger.getLogger(LoaderTest.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	public void compareImage(String expected, String type, BufferedImage image){
		try {
			BufferedImage expectedImage = ImageIO.read(new File(imageDir+type+"_"+expected));
			DataBuffer exp = imageToBuffer(expectedImage);
			DataBuffer act = imageToBuffer(image);
			assertEquals(type+" " +expected, exp.getSize(), act.getSize());

			for (int i = 0, max = exp.getSize(); i < max; i++) {
			  int e = exp.getElem(i);
			  int a = act.getElem(i);
			  assertEquals(type +" " +expected+ " " + i,e,a);
			}
		} catch (IOException ex) {
			fail(ex.getMessage());
		}
	}

	public DataBuffer imageToBuffer(BufferedImage image){
		Raster r = image.getData();
		DataBuffer db = r.getDataBuffer();
		return(db);
	}
}
