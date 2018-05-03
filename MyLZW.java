package P2;

/*************************************************************************
 *  Compilation:  javac LZW.java
 *  Execution:    java LZW - < input.txt   (compress)
 *  Execution:    java LZW + < input.txt   (expand)
 *  Dependencies: BinaryIn.java BinaryOut.java
 *
 *  Compress or expand binary input from standard input using LZW.
 *
 *  WARNING: STARTING WITH ORACLE JAVA 6, UPDATE 7 the SUBSTRING
 *  METHOD TAKES TIME AND SPACE LINEAR IN THE SIZE OF THE EXTRACTED
 *  SUBSTRING (INSTEAD OF CONSTANT SPACE AND TIME AS IN EARLIER
 *  IMPLEMENTATIONS).
 *
 *  See <a href = "http://java-performance.info/changes-to-string-java-1-7-0_06/">this article</a>
 *  for more details.
 *
 *************************************************************************/

public class MyLZW {

    private static final int R = 256;        // number of input chars
    private static int L = 512;
    private static int W = 9;
    private static char mode = ' ';
    private static TST<Integer> compst;
    private static String[] expst;
    private static int code;
    private static int size = 0;
    private static double compressedProcessed = 0;
    private static double unCompressedProcessed = 0;
    private static double oldRatio = 0;
    private static double newRatio = 0;
    private static double threshold = 0;
    private static String[] arguments;
    private static int i;
    private static int unCompressedFileSize;

    public static void compress() { 

        String input = BinaryStdIn.readString();
        compst = new TST<Integer>();
        for (i = 0; i < R; i++)
            compst.put("" + (char) i, i);
        code = R+1;  // R is codeword for EOF

        BinaryStdOut.write(mode, W);

        while (input.length() > 0) {
            if((compst.size()==L-1)&&W<16)
            {
                W++;
                L*=2;
            }
            else if(compst.size()==65535&&mode=='r') 
            {
                //System.err.println("COMPRESSING IN RESET MODE");
                resetComp();
            }
            else if(compst.size()==65535&&mode=='m')
            {
               // System.err.println("COMPRESSING IN MONITOR MODE");
                monitor();
            }
            //System.err.println(compst.size());
            String s = compst.longestPrefixOf(input);  // Find max prefix match s.
            //unCompressedProcessed+=(unCompressedFileSize-input.length());
            unCompressedProcessed+=((s.getBytes().length)*8);
            //System.err.println("unCompressedProcessed: "+unCompressedProcessed);
            BinaryStdOut.write(compst.get(s), W);      // Print s's encoding.
            compressedProcessed+=W;
            //System.err.println("CompressedProcessed: "+compressedProcessed);
            int t = s.length();
            //System.err.println("Length of s: "+s.length()+"          input length: "+ input.length());
            //System.err.println("value of code: "+code+"       value of L: "+L);
            if (t < input.length() && code < L)    // Add s to symbol table.
                compst.put(input.substring(0, t + 1), code++);
            input = input.substring(t);            // Scan past s in input.
            //System.err.println("TST Size: "+compst.size());

        }
        BinaryStdOut.write(R, W);
        BinaryStdOut.close();
    } 

    public static void expand() {

        L = 65536;       // number of codewords = 2^W
        size = 512;      // initialize counter to tell when to move up codeword lengths
        W = 9;         // codeword width

        expst = new String[L];

        // initialize symbol table with all 1-character strings
        for (i = 0; i < R; i++)
        {
            expst[i] = "" + (char) i;
        }
        expst[i++] = "";                        // (unused) lookahead for EOF

        int codeword = BinaryStdIn.readInt(W);
        compressedProcessed+=W;
        if (codeword == R) return;           // expanded message is empty string
        if(codeword=='r')
        {
           // System.err.println("FOUND THE RESET KEY");
            mode = 'r';
        }
        else if(codeword=='m')
        {
            //System.err.println("FOUND THE MONITOR KEY");
            mode = 'm';
        }
        codeword = BinaryStdIn.readInt(W);
        compressedProcessed+=W;
        String val = expst[codeword];

        while (true) {
            //System.err.println("value of L: "+L+"    value of i: "+i);
            if(i == 65535 && mode=='r')
            {
                //System.err.println("Reset at word "+val);

                //System.err.println("RESETTING IN EXPANSION");

                resetExp();
            }
            else if(i == 65535 && mode=='m')
            {
                //System.err.println("MONITORING IN EXPANSION");
                monitor();
            }
            BinaryStdOut.write(val);
            unCompressedProcessed+=(val.getBytes().length*8);
            codeword = BinaryStdIn.readInt(W);
            compressedProcessed+=W;
            if (codeword == R) break;
            String s = expst[codeword];
            if (i == codeword) s = val + val.charAt(0);   // special case hack
            //System.err.println("S: "+s);
            if (i < L) expst[i++] = val + s.charAt(0);
            val = s;
            if(i==size-1&&W<16)
            {
                W++;
                size*=2;
            }
        }
        BinaryStdOut.close();
    }

    public static void main(String[] args) {
        arguments = args;
        if(args.length>1)
        { 
            if           (args[1].equals("n")) mode = 'n';
            else if      (args[1].equals("r")) mode = 'r';
            else if      (args[1].equals("m")) mode = 'm';
        }
        if          (args[0].equals("-")) compress();
        else if     (args[0].equals("+")) expand();
        else        throw new IllegalArgumentException("Illegal command line argument");
    }

    public static void monitor() 
    {
        newRatio = unCompressedProcessed / compressedProcessed;
        //System.err.println("newRatio: "+newRatio);
        //System.err.println("oldRatio: "+oldRatio);

        if(oldRatio!=0)
        {
            threshold = (double)oldRatio/(double)newRatio;
            //System.err.println("threshold: "+threshold);
        }
        else
        {
            oldRatio = newRatio;
            newRatio = 0;
        }
        if(threshold>1.1)
        {
            threshold = 0;
            if(arguments.length>1)
            {
                resetComp();
            }
            else
            {
                resetExp();
            }
        }
    }
    public static void resetComp(){
    	i = 0;
        W = 9;
        L = 512;
        code = R+1;
        // reinitialize the TST structure
        compst = new TST<Integer>();
        // repopulate the first 256 ascii characters 
        for (i = 0; i < R; i++)
        {
            compst.put("" + (char) i, i);
        }
    }
    public static void resetExp(){
    	i = 0;
        L = 65536;       // number of codewords = 2^W
        size = 512;      // initialize counter to tell when to move up codeword lengths
        W = 9;         // codeword width
        

        expst = new String[65536];
        // initialize symbol table with all 1-character strings
        for (i = 0; i < R; i++)
        {
            expst[i] = "" + (char) i;
        }
        expst[i++] = "";

    }
}
