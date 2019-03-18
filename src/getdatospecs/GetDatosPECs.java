/*
 * Get data from PECs app
 */
package getdatospecs;

/**
 * Get Data from PDF forms
 * @author R Sesma
 */

import java.io.IOException;
import com.itextpdf.text.DocumentException;
import javax.swing.*;

public class GetDatosPECs {

    public static String dir;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, DocumentException {
        
        boolean lContinue = false;
        boolean lST = false;
        
        String type = "";
        
        if (args.length == 0) {
            //No arguments: ask for a folder to continue
            JFrame frame = new JFrame("JFileChooser dialog window");        //JFrame for the file chooser dialog

            JFileChooser folderDlg = new JFileChooser();
            String defDir = System.getProperty("user.home");
            folderDlg.setCurrentDirectory(new java.io.File(defDir));        //default folder
            folderDlg.setDialogTitle("Indicar carpeta con las PECs");       //dialog title
            folderDlg.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);  //choose only folders
            folderDlg.setAcceptAllFileFilterUsed(false);        //disable the "All files" option

            if (folderDlg.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) { 
                dir = folderDlg.getSelectedFile().getAbsolutePath();     //selected folder
                lContinue = true;
            }
        }
        else {
            /**Arguments call
             * For command-line calls, there are 2 options:
             * - only one argument: general call, argument is PECs folder
             * - +1 argument: ST call, 1st argument is folder/dir, 2nd is type of operation
             */
            lContinue = true;
            dir = args[0];
            if (args.length > 1) {
                lST = true;
                type = args[1];
            }
        }
        
        ExtractMethods extract = new ExtractMethods();
        if (lContinue) {
            if (!lST) {
                extract.getDatosGeneral(dir);           //general extract data method
            } 
            else {
                //ST special methods
                if (type.equals("getPEC1")) extract.getNotaPEC1(dir);
                if (type.equals("getPEC")) extract.getDatosPEC_ST(dir,args[2]);
                if (type.equals("getST")) extract.getDatosST(dir,Integer.parseInt(args[2]),Integer.parseInt(args[3]));
                
                if (type.equals("entregahonorPEC1")) extract.getEntregaHonorPEC1(dir);
                if (type.equals("entregahonor")) extract.getEntregaHonor(dir,args[2],args[3]);
                if (type.equals("honorIO")) extract.getHonorIO(dir);
                
                if (type.equals("sintaxisST1")) extract.getSintaxisST1(dir);
                if (type.equals("sintaxisST2")) extract.getSintaxisST2(dir);
                
                if (type.equals("rename")) extract.renameFormFields(dir);
                if (type.equals("test")) extract.test(dir);
                
                if (type.equals("loadAccess")) extract.loadAccess(dir);
            }
        }

        System.exit(0);
       
        //System.out.println(key);
    }
}
