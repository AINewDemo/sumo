package org.geoimage.viewer.util.files;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

public class SarFileUtil {

	/**
	 * 
	 * @param imagesFolder
	 * @return
	 */
	public static List<File> scanFolderForImages(File imagesFolder){
		List<File> imgFiles=new ArrayList<File>();
		//list only folders
		File[] childs=imagesFolder.listFiles(new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
				

		for(File f:childs){
			File[] imgFile=f.listFiles(new SarImageFileFilter());
			if(imgFile.length==1){
				imgFiles.add(imgFile[0]);
			}
		}
		
		
		
		return imgFiles;
	}
	
	
}
