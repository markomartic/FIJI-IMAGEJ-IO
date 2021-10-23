/*-
 * #%L
 * IO plugin for Fiji.
 * %%
 * Copyright (C) 2008 - 2021 Fiji developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
//20140703 - Jerome Parent
//Reader for Koala binary generated by Lyncee Tec software
//This plugin read .bin file, scale the XY value in microns and the Z value in nm
//Based on reader tutorial provided by Albert Cardona : http://albert.rierol.net/imagej_programming_tutorials.html

package sc.fiji.io;

import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;
import java.awt.*;
import java.io.*;
import java.nio.*;
import ij.io.*;

public class Koala_Bin_Reader extends ImagePlus implements PlugIn {  
      
    public void run(String arg) {  
        String path = getPath(arg);  
        if (null == path) return;  
        if (!parse(path)) return;  
        if (null == arg || 0 == arg.trim().length()) this.show(); // was opened by direct call to the plugin  
                          // not via HandleExtraFileTypes which would  
                          // have given a non-null arg.  
    }  
  
    /** Accepts URLs as well. */  
    private String getPath(String arg) {  
        if (null != arg) {  
            if (0 == arg.indexOf("http://")  
             || new File(arg).exists()) return arg;  
        }  
        // else, ask:  
        OpenDialog od = new OpenDialog("Choose a .bin file", null);  
        String dir = od.getDirectory();  
        if (null == dir) return null; // dialog was canceled  
        dir = dir.replace('\\', '/'); // Windows safe  
        if (!dir.endsWith("/")) dir += "/";  
        return dir + od.getFileName();  
    }  
  
    /** Opens URLs as well. */  
    private InputStream open(String path) throws Exception {  
        if (0 == path.indexOf("http://"))  
            return new java.net.URL(path).openStream();  
        return new FileInputStream(path);  
    }  
  
    private boolean parse(String path) {  
        // Open file and read header 
		// header size 23 bytes
		byte[] buf = new byte[23];  
        try {  
            InputStream stream = open(path);  
            stream.read(buf, 0, 23);  
            stream.close();  
        } catch (Exception e) {  
            e.printStackTrace();  
            return false;  
        }
			byte version = buf[0];
			byte endianness = buf[1];
			int header_size = readIntLittleEndian(buf,2);
			int width = readIntLittleEndian(buf,6);  
			int height = readIntLittleEndian(buf,10);
			float pixel_size = readFloatLittleEndian(buf,14);
			float height_conv = readFloatLittleEndian(buf,18);
			byte unit = buf[22];	
       
        // Build a new FileInfo object with all file format parameters and file data  
        FileInfo fi = new FileInfo();  
        fi.fileType = fi.GRAY32_FLOAT;  
        fi.fileFormat = fi.RAW;  
        int islash = path.lastIndexOf('/');  
        if (0 == path.indexOf("http://")) {  
            fi.url = path;  
        } else {  
            fi.directory = path.substring(0, islash+1);  
        }  
        fi.fileName = path.substring(islash+1);  
        fi.width = width;  
        fi.height = height;  
		// conversion XY in microns
		fi.pixelHeight = (double)pixel_size*1e6;
		fi.pixelWidth = (double)pixel_size*1e6;
		fi.unit = "um";
		if (unit > 0) // only for phase image
			fi.valueUnit = "nm";
        fi.nImages = 1;  
        fi.gapBetweenImages = 0;  
		if(endianness == 0)
			fi.intelByteOrder = true; // little endian 
		else	
			fi.intelByteOrder = false;
        fi.whiteIsZero = false; // no inverted LUT  
        fi.longOffset = fi.offset = header_size; // header size, in bytes  
  
        // Now make a new ImagePlus out of the FileInfo  
        // and integrate its data into this PlugIn, which is also an ImagePlus  
        try {  
            FileOpener fo = new FileOpener(fi);  
            ImagePlus imp = fo.open(false); 
			ImageProcessor temp = imp.getProcessor();
			
			//set vertical scale to phase image
			if (unit > 0){	// only for phase image
				temp.multiply(height_conv*1e9);
				imp.setProcessor(temp);
			}
            this.setStack(imp.getTitle(), imp.getStack());  
            this.setCalibration(imp.getCalibration());  
            Object obinfo = imp.getProperty("Info");  
            if (null != obinfo) this.setProperty("Info", obinfo);  
            this.setFileInfo(imp.getOriginalFileInfo());
        } catch (Exception e) {  
            e.printStackTrace();  
            return false;  
        } 
        return true;  
    }
	private final int readIntLittleEndian(byte[] buf, int start) { 
		byte temp[] = new byte[4];
		for (int i=0;i<4;i++)
			temp[i] = buf[start+i];
		return ByteBuffer.wrap(temp).order(ByteOrder.LITTLE_ENDIAN ).getInt(); 
    }  
	private final float readFloatLittleEndian(byte[] buf, int start) {
		byte temp[] = new byte[4];
		for (int i=0;i<4;i++)
			temp[i] = buf[start+i];
		return ByteBuffer.wrap(temp).order(ByteOrder.LITTLE_ENDIAN ).getFloat();
	}
}  
