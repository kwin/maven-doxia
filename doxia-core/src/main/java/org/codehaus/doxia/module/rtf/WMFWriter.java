/*
 * Copyright (c) 2001 Pixware. 
 *
 * Author: Jean-Yves Belmonte (john@codehaus.org)
 *
 * This file is part of the Pixware doxia package.
 * For conditions of use and distribution, see the attached legal.txt file.
 */

package org.codehaus.doxia.module.rtf;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Vector;

/**
 * A Windows MetaFile writer.
 */
public class WMFWriter
{

    /*
     * See the libwmf library documentation
     * (http://www.wvware.com/wmf_doc_index.html)
     * for a description of WMF format.
     */

    private static Record trailer = new Record( 0, null );

    /*
     * standard header fields
     */
    private short fileType;
    private short headerSize;
    private short version;
    private int fileSize;
    private short numOfObjects;
    private int maxRecordSize;
    private short numOfParams;

    private Vector records;

    public WMFWriter()
    {
        fileType = 2;
        headerSize = 9;
        version = 0x0300;
        fileSize = headerSize + trailer.size();
        numOfObjects = 0;
        maxRecordSize = trailer.size();
        numOfParams = 0;

        records = new Vector();
    }

    public void add( Record record )
    {
        records.addElement( record );

        int size = record.size();
        fileSize += size;
        if ( size > maxRecordSize ) maxRecordSize = size;
    }

    public int size()
    {
        return fileSize;
    }

    public void write( String fileName ) throws IOException
    {
        BufferedOutputStream out =
            new BufferedOutputStream( new FileOutputStream( fileName ) );
        write( out );
        out.flush();
        out.close();
    }

    public void write( OutputStream out ) throws IOException
    {
        write16( fileType, out );
        write16( headerSize, out );
        write16( version, out );
        write32( fileSize, out );
        write16( numOfObjects, out );
        write32( maxRecordSize, out );
        write16( numOfParams, out );

        for ( int i = 0, n = records.size(); i < n; ++i )
        {
            Record record = (Record) records.elementAt( i );
            record.write( out );
        }

        trailer.write( out );
    }

    /**
     * Writes a 16-bit integer in little-endian format.
     */
    public static void write16( int word, OutputStream out )
        throws IOException
    {
        out.write( word );
        out.write( word >> 8 );
    }

    /**
     * Writes a 32-bit integer in little-endian format.
     */
    public static void write32( int dword, OutputStream out )
        throws IOException
    {
        out.write( dword );
        out.write( dword >> 8 );
        out.write( dword >> 16 );
        out.write( dword >> 24 );
    }

    public void print( Writer out ) throws IOException
    {
        print16( fileType, out );
        print16( headerSize, out );
        print16( version, out );
        print32( fileSize, out );
        print16( numOfObjects, out );
        print32( maxRecordSize, out );
        print16( numOfParams, out );
        out.write( System.getProperty( "line.separator" ) );

        for ( int i = 0, n = records.size(); i < n; ++i )
        {
            Record record = (Record) records.elementAt( i );
            record.print( out );
        }

        trailer.print( out );
    }

    public static void print16( int word, Writer out ) throws IOException
    {
        byte[] buf = new byte[2];
        buf[0] = (byte) word;
        buf[1] = (byte) ( word >> 8 );
        print( buf, 0, 2, out );
    }

    public static void print32( int dword, Writer out ) throws IOException
    {
        byte[] buf = new byte[4];
        buf[0] = (byte) dword;
        buf[1] = (byte) ( dword >> 8 );
        buf[2] = (byte) ( dword >> 16 );
        buf[3] = (byte) ( dword >> 24 );
        print( buf, 0, 4, out );
    }

    public static void print( byte[] buf, int off, int len, Writer out )
        throws IOException
    {
        char[] cbuf = new char[2 * len];

        for ( int i = off, j = 0, n = off + len; i < n; ++i )
        {
            int d = ( buf[i] >> 4 ) & 0x0f;
            if ( d < 10 )
                cbuf[j++] = (char) ( '0' + d );
            else
                cbuf[j++] = (char) ( 'a' + ( d - 10 ) );
            d = buf[i] & 0x0f;
            if ( d < 10 )
                cbuf[j++] = (char) ( '0' + d );
            else
                cbuf[j++] = (char) ( 'a' + ( d - 10 ) );
        }

        out.write( cbuf );
    }

    public static void print( byte[] buf, int off, int len, Writer out, int lw )
        throws IOException
    {
        String ls = System.getProperty( "line.separator" );
        for ( int i = off; len > 0; )
        {
            int n = Math.min( len, lw / 2 );
            print( buf, i, n, out );
            out.write( ls );
            len -= n;
            i += n;
        }
    }

    /**
     * Standard data record.
     */
    static public class Record
    {

        protected int size;
        private short function;
        private short[] parameters;

        public Record( int function, int[] parameters )
        {
            this.function = (short) function;
            if ( parameters != null )
            {
                this.parameters = new short[parameters.length];
                for ( int i = 0; i < parameters.length; ++i )
                    this.parameters[i] = (short) parameters[i];
            }
            size = 3 + ( parameters == null ? 0 : parameters.length );
        }

        public int size()
        {
            return size;
        }

        public void write( OutputStream out ) throws IOException
        {
            write32( size, out );
            write16( function, out );
            if ( parameters != null )
                for ( int i = 0; i < parameters.length; ++i )
                    write16( parameters[i], out );
        }

        public void print( Writer out ) throws IOException
        {
            print32( size, out );
            print16( function, out );
            if ( parameters != null )
                for ( int i = 0; i < parameters.length; ++i )
                    print16( parameters[i], out );
        }

    }

    /**
     * DIB data structure.
     */
    static public class Dib
    {

        /*
         * compression types
         */
        public static final int BI_RGB = 0;
        public static final int BI_RLE8 = 1;
        public static final int BI_RLE4 = 2;
        public static final int BI_BITFIELDS = 3;

        /*
         * information header fields
         */
        public final int biSize = 40;		// header size
        public int biWidth;			// image width
        public int biHeight;			// image height
        public final short biPlanes = 1;	// number of planes
        public short biBitCount;		// number of bits per pixel
        public int biCompression;		// compression type
        public int biSizeImage;			// image data size
        public int biXPelsPerMeter;		// horizontal resolution
        public int biYPelsPerMeter;		// vertical resolution
        public int biClrUsed;			// number of colors
        public int biClrImportant;		// number of required colors

        public byte[] palette;			// color palette
        public byte[] bitmap;			// bitmap data

        public int size()
        {
            int size = biSize;
            if ( palette != null ) size += palette.length;
            if ( bitmap != null )
            {
                if ( biSizeImage != 0 )
                    size += biSizeImage;
                else
                    size += bitmap.length;
            }
            return size / 2;
        }

        public void write( OutputStream out ) throws IOException
        {
            write32( biSize, out );
            write32( biWidth, out );
            write32( biHeight, out );
            write16( biPlanes, out );
            write16( biBitCount, out );
            write32( biCompression, out );
            write32( biSizeImage, out );
            write32( biXPelsPerMeter, out );
            write32( biYPelsPerMeter, out );
            write32( biClrUsed, out );
            write32( biClrImportant, out );
            if ( palette != null ) out.write( palette );
            if ( bitmap != null )
            {
                if ( biSizeImage != 0 )
                    out.write( bitmap, 0, biSizeImage );
                else
                    out.write( bitmap );
            }
        }

        public void print( Writer out ) throws IOException
        {
            String ls = System.getProperty( "line.separator" );

            print32( biSize, out );
            print32( biWidth, out );
            print32( biHeight, out );
            print16( biPlanes, out );
            print16( biBitCount, out );
            out.write( ls );

            print32( biCompression, out );
            print32( biSizeImage, out );
            print32( biXPelsPerMeter, out );
            print32( biYPelsPerMeter, out );
            print32( biClrUsed, out );
            print32( biClrImportant, out );
            out.write( ls );

            if ( palette != null )
                WMFWriter.print( palette, 0, palette.length, out, 64 );

            if ( bitmap != null )
            {
                int len = ( biSizeImage != 0 ) ? biSizeImage : bitmap.length;
                WMFWriter.print( bitmap, 0, len, out, 76 );
            }
        }

        public static int rlEncode8( byte[] inBuf, int inOff, int inLen,
                                     byte[] outBuf, int outOff )
        {
            int i1, i2, j, k, n;
            int len;

            for ( i1 = inOff, j = outOff, n = ( inOff + inLen ); i1 < n; )
            {
                for ( i2 = ( i1 + 1 ), len = 1; i2 < n; ++i2, ++len )
                    if ( inBuf[i2] != inBuf[i2 - 1] )
                        break;

                if ( len > 1 )
                {
                    while ( len > 255 )
                    {
                        outBuf[j++] = (byte) 255;
                        outBuf[j++] = inBuf[i1];
                        len -= 255;
                    }
                    if ( len > 0 )
                    {
                        outBuf[j++] = (byte) len;
                        outBuf[j++] = inBuf[i1];
                    }
                    i1 = i2;
                    continue;
                }

                for ( ++i2; i2 < n; ++i2, ++len )
                    if ( inBuf[i2] == inBuf[i2 - 1] )
                        break;

                while ( len > 255 )
                {
                    outBuf[j++] = 0;
                    outBuf[j++] = (byte) 255;
                    for ( k = 0; k < 255; ++k )
                        outBuf[j++] = inBuf[i1++];
                    outBuf[j++] = (byte) 0;
                    len -= 255;
                }

                if ( len > 2 )
                {
                    outBuf[j++] = 0;
                    outBuf[j++] = (byte) len;
                    for ( k = 0; k < len; ++k )
                        outBuf[j++] = inBuf[i1++];
                    if ( len % 2 != 0 )
                        outBuf[j++] = 0;
                }
                else
                {
                    while ( len > 0 )
                    {
                        outBuf[j++] = 1;
                        outBuf[j++] = inBuf[i1++];
                        len -= 1;
                    }
                }
            }

            return j - outOff;
        }

    }

    static public class DibBitBltRecord extends Record
    {

        /*
         * parameter count
         */
        public static final int P_COUNT = 8;

        /*
         * parameter indexes
         */
        public static final int P_ROP_L = 0;
        public static final int P_ROP_H = 1;
        public static final int P_YSRC = 2;
        public static final int P_XSRC = 3;
        public static final int P_HEIGHT = 4;
        public static final int P_WIDTH = 5;
        public static final int P_YDST = 6;
        public static final int P_XDST = 7;

        private Dib dib;

        public DibBitBltRecord( int[] parameters, Dib dib )
        {
            super( 0x0940, parameters );
            size += dib.size();
            this.dib = dib;
        }

        public void write( OutputStream out ) throws IOException
        {
            super.write( out );
            dib.write( out );
        }

        public void print( Writer out ) throws IOException
        {
            super.print( out );
            dib.print( out );
        }

    }

}