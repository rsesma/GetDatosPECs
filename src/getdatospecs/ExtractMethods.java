/*
 * Class to extract data from PECs
 */
package getdatospecs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

/**
 *
 * @author R Sesma
 */

public class ExtractMethods {
    
    public static String newline = System.getProperty("line.separator");
    
    public void getDatosGeneral(String dir) throws IOException {
        
        double time = System.currentTimeMillis();           //timer
        
        //Get all the pdf files of dir
        File folder = new File(dir);
        FilenameFilter pdfFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                if (lowercaseName.endsWith(".pdf")) {
                    return true;
                } else {
                    return false;
                }
            }
        };
        File[] listOfFiles = folder.listFiles(pdfFilter);
        
        boolean lProblems = false;
        boolean lComments = false;
        boolean lfirst = true;
        List<String> lines = new ArrayList<String>();
        List<String> comments = new ArrayList<String>();
        List<String> problems = new ArrayList<String>();
        List<String> names = new ArrayList<String>();
        for (File file : listOfFiles) {
            if (file.isFile()) {                
                String n = file.getName();
                String dni = n.substring(n.lastIndexOf("_")+1,n.lastIndexOf("."));

                PdfReader reader = new PdfReader(file.getAbsolutePath());
                AcroFields form = reader.getAcroFields();
                
                if (form.getFields().size()>0) {
                    if (lfirst) {
                        //Get form fields names and sort alphabetically
                        for (String key : form.getFields().keySet()) {
                            if (key.substring(0, 1).equalsIgnoreCase("P")) names.add(key);
                        }
                        Collections.sort(names);
                        lfirst = false;
                    }
                    
                    //Build COMMENTS section
                    if (!form.getField("COMENT").isEmpty()) {
                        lComments = true;
                        comments.add(dni + ":" + form.getField("COMENT") + "\n");
                    }
                    //Header with identification data
                    String c = "'" + form.getField("APE1") + "','" + form.getField("APE2") + "','" + 
                            form.getField("NOMBRE") + "','" + dni + "'" +
                            ((form.getField("HONOR").equalsIgnoreCase("Yes")) ? ",1" : ",0");
                    
                    //Loop through the sorted fields and get the contents
                    for (String name : names) {
                        c = c + ",'" + form.getField(name).replace(".", ",") + "'";
                    }
                    lines.add(c);
                }
                else {
                    //If there are no fields on the form, the PDF file is corrupted
                    lProblems = true;
                    problems.add(dni);
                }
            }
        }
        //Save data
        Path fdata = Paths.get(dir + "/datos_pecs.txt");
        Files.write(fdata, lines, Charset.forName("UTF-8"));
        //Save comments, if any
        if (lComments) {
            Path fcom = Paths.get(dir + "/comentarios.txt");
            Files.write(fcom, comments, Charset.forName("UTF-8"));
        }
        //Save problems, if any
        if (lProblems) {
            Path fprob = Paths.get(dir + "/problemas.txt");
            Files.write(fprob, problems, Charset.forName("UTF-8"));
        }
        
        double d = (System.currentTimeMillis() - time)/1000;
        String message = "Proceso finalizado (" +  String.format("%.1f", d) +" segs)." ;
        if (lProblems) message = message + newline + "Hay problemas.";
        if (lComments) message = message + newline + "Hay comentarios.";
        JOptionPane.showMessageDialog(null,message);
    }
    
    public void getSintaxisST1(String dir) throws IOException {
        
        double time = System.currentTimeMillis();
        
        File folder = new File(dir + "/originales");
        FilenameFilter pdfFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                if (lowercaseName.endsWith(".pdf")) {
                    return true;
                } else {
                    return false;
                }
            }
        };
        File[] listOfFiles = folder.listFiles(pdfFilter);

        for (File file : listOfFiles) {
            if (file.isFile()) {
                String n = file.getName();
                String dni = n.substring(n.lastIndexOf("_")+1,n.lastIndexOf(".pdf"));

                PdfReader reader = new PdfReader(file.getAbsolutePath());
                AcroFields form = reader.getAcroFields();
                
                String c = "**PEC : " + dni + newline + 
                        "cd \"C:\\Users\\reed\\Desktop\\PECs\\ST1\\PEC2\"" + newline +
                        "import excel PEC2_ST1.xlsx, sheet(\"Datos\") firstrow" + 
                        newline + newline;
                
                for(int i=2; i<=21; i++){
                    String p = ((i<10) ? "0"+Integer.toString(i) : Integer.toString(i));
                    if (i!=4 && i!=9 && i!=11 && i!=20) {
                        c = c + "*Pregunta " + p + newline + form.getField("P"+p+"_B" ) + newline + newline;
                    }
                    if (i==8) {
                        c = c + "merge 1:1 Id using \"PEC2_ST1_A.dta\", nogenerate" + newline + 
                                "testvars Sexo FNc FRc FN FR Edad BajoPeso, p(3 5 5 6 6 7 8) id(Id)" + newline + newline;
                    }
                    if (i==13) {
                        c = c + "merge 1:1 Id using \"PEC2_ST1_B.dta\", nogenerate" + newline +
                                "testvars pCardio APGAR, p(12 13) id(Id)" + newline + newline;
                    }
                    if (i==14) {
                        c = c + "testvars APGARbajo, p(14) v(_APGARbajo0) id(Id)" + newline + newline;
                    }
                    if (i==17) {
                        c = c + "testvars APGARbajo k GFR, p(15 16 17) id(Id)" + newline + newline;
                    }
                }
                reader.close();
                
                try( PrintWriter out = new PrintWriter(dir + "/sintaxis/" + dni + ".do") ){
                    out.println( c );
                }
            }
        }
        
        double t = (System.currentTimeMillis() - time)/1000;
        JOptionPane.showMessageDialog(null,"Proceso finalizado (" +  String.format("%.1f", t) + " segs).");        
    }

    public void getSintaxisST2(String dir) throws IOException {
        
        double time = System.currentTimeMillis();
        
        File folder = new File(dir + "/originales");
        FilenameFilter pdfFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                if (lowercaseName.endsWith(".pdf")) {
                    return true;
                } else {
                    return false;
                }
            }
        };
        File[] listOfFiles = folder.listFiles(pdfFilter);

        for (File file : listOfFiles) {
            if (file.isFile()) {                
                String n = file.getName();
                String dni = n.substring(n.lastIndexOf("_")+1,n.lastIndexOf(".pdf"));

                PdfReader reader = new PdfReader(file.getAbsolutePath());
                AcroFields form = reader.getAcroFields();
                
                String c = "**PEC : " + dni + newline +
                        "cd \"C:\\Users\\reed\\Desktop\\PECs\\ST2\"" + newline +
                        "erase Recaidas.dta" + newline +
                        "erase Censal.dta" + newline +
                        "erase Temporal.dta" + newline +
                        "import excel \"PEC1_ST2.xlsx\", sheet(\"Censal\") firstrow clear" + 
                        newline + newline;
                
                for(int i=2; i<=16; i++){
                    String p = ((i<10) ? "0"+Integer.toString(i) : Integer.toString(i));
                    if (i<16) c = c + "*Pregunta " + p + newline + form.getField("P"+p+"_B" ) + newline + newline;
                    if (i==16) c = c + "*Pregunta " + p + newline + "clear" + newline + form.getField("P"+p+"_B" ) + newline + newline;
                    if (i==7) {
                        c = c + "merge 1:1 IdPac FR using \"PEC1_ST2_A.dta\", nogenerate" + newline + 
                                "testvars Num NRec DiasRec CambioGluc, p(6 6 7 7) id(IdPac FR)" + newline + 
                                "drop _*" + newline + newline;
                    }
                    if (i==9) {
                        c = c + "merge 1:1 IdPac using \"PEC1_ST2_B.dta\", nogenerate" + newline + 
                                "testvars RecMin RecMax RecMed, p(9 9 9) id(IdPac)" + newline + newline;
                    }
                    if (i==14) {
                        c = c + "merge 1:1 IdPac using \"PEC1_ST2_C.dta\", nogenerate" + newline + 
                                "testvars DiasSeg Recaida, p(14 14) id(IdPac)" + newline + 
                                "drop _*" + newline + newline;
                    }
                }
                reader.close();
                
                try( PrintWriter out = new PrintWriter(dir + "/sintaxis/" + dni + ".do") ){
                    out.println( c );
                }
            }
        }
        
        double t = (System.currentTimeMillis() - time)/1000;
        JOptionPane.showMessageDialog(null,"Proceso finalizado (" +  String.format("%.1f", t) + " segs).");        
    }

    public void getDatosPEC_ST(String dir, String cPeriodo) throws IOException {

        String dni = dir.substring(dir.lastIndexOf("_")+1,dir.lastIndexOf(".pdf"));
        String cCurso = dir.substring(dir.lastIndexOf("_")-3,dir.lastIndexOf("_"));
        
        PdfReader reader = new PdfReader(dir);
        AcroFields form = reader.getAcroFields();
        
        List<String> names = new ArrayList<String>();
        for (String key : form.getFields().keySet()) {
            if (key.substring(0, 1).equalsIgnoreCase("P")) names.add(key);
        }
        Collections.sort(names);
        
        List<String> lines = new ArrayList<String>();
        for (String name : names) {
            String p = name.substring(1);
            String v = form.getField(name).replace(".", ",");

            if (v.length()<6) {
                String c = "INSERT INTO PEC_respuestas (Periodo,Curso,DNI,Pregunta,respuesta) VALUES (" + "'" + cPeriodo + "', '" + cCurso + "', '" + dni + "', '" + p + "', '" + v + "');";
                lines.add(c);
            }
        }
        reader.close();
        
        Path fdata = Paths.get(dir.replace(".pdf",".txt"));
        Files.write(fdata, lines, Charset.forName("UTF-8"));
    }
    
    public String getDatosPEC(String dir, int nFields, int nExcept) throws IOException {

        String dni = dir.substring(dir.lastIndexOf("_")+1,dir.lastIndexOf(".pdf"));
        
        PdfReader reader = new PdfReader(dir);
        AcroFields form = reader.getAcroFields();
        
        //Header with identification data
        String c = "'" + form.getField("APE1") + "','" + form.getField("APE2") + "','" + 
            form.getField("NOMBRE") + "','" + dni + "'" + 
            ((form.getField("HONOR").equalsIgnoreCase("Yes")) ? ",1" : ",0");

        String sep = ",";
        String del = "'";
        for(int i=1; i<=nFields; i++){
            String p = ((i<10) ? "0"+Integer.toString(i) : Integer.toString(i));
            if (i!=nExcept) c = c + sep + del + form.getField("P"+p+"_A") + del;
            if (i==nExcept) c = c + sep + del + form.getField("P"+p+"_A").replace(".",",") + del + 
                               sep + del + form.getField("P"+p+"_B").replace(".",",") + del +
                               sep + del + form.getField("P"+p+"_C").replace(".",",") + del;
        }
        reader.close();
        
        return c;
    }
    
    public void getDatosST(String dir, int nFields, int nExcept) throws IOException {
        
        double time = System.currentTimeMillis();
        
        File folder = new File(dir);
        FilenameFilter pdfFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                if (lowercaseName.endsWith(".pdf")) {
                    return true;
                } else {
                    return false;
                }
            }
        };
        File[] listOfFiles = folder.listFiles(pdfFilter);

        List<String> lines = new ArrayList<String>();
        for (File file : listOfFiles) {
            if (file.isFile()) {
                String c = this.getDatosPEC(file.getAbsolutePath(), nFields, nExcept);
                lines.add(c);
            }
        }
        //Save data
        Path fdata = Paths.get(dir + "/datos_pecs.txt");
        Files.write(fdata, lines, Charset.forName("UTF-8"));
        
        double t = (System.currentTimeMillis() - time)/1000;
        JOptionPane.showMessageDialog(null,"Proceso finalizado (" +  String.format("%.1f", t) + " segs).");        
    }
    
    public void getNotaPEC1(String dir) throws IOException {
        
        File folder = new File(dir);
        FilenameFilter pdfFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                if (lowercaseName.endsWith(".pdf")) {
                    return true;
                } else {
                    return false;
                }
            }
        };
        File[] listOfFiles = folder.listFiles(pdfFilter);

        String c = "";
        for (File file : listOfFiles) {
            if (file.isFile()) {                
                PdfReader reader = new PdfReader(file.getAbsolutePath());
                AcroFields form = reader.getAcroFields();
                c = form.getField("NOTA");
                reader.close();
            }
        }
        
        try( PrintWriter out = new PrintWriter(dir + "/nota.txt") ){
            out.println( c );
        }

    }
    
    public void renameFormFields(String dir) throws IOException, DocumentException {
        
        double time = System.currentTimeMillis();
        
        File folder = new File(dir + "/originales");
        FilenameFilter pdfFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                if (lowercaseName.endsWith(".pdf")) {
                    return true;
                } else {
                    return false;
                }
            }
        };
        File[] listOfFiles = folder.listFiles(pdfFilter);

        for (File file : listOfFiles) {
            if (file.isFile()) {                
                String n = file.getName();
                String dni = n.substring(n.lastIndexOf("_")+1,n.lastIndexOf(".pdf"));

                PdfReader reader = new PdfReader(file.getAbsolutePath(),"leamst1".getBytes());
                PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(dir + "/clonadas/PEC1_ST2_" + dni + ".pdf"));
                AcroFields form = stamper.getAcroFields();
                form.renameField("Widget", "P01_B");
                form.renameField("_2", "P02_B");
                form.renameField("_3", "P03_B");
                form.renameField("_4", "P04_B");
                form.renameField("_5", "P05_B");
                form.renameField("_9", "P06_B");
                form.renameField("_6", "P07_B");
                form.renameField("_7", "P08_B");
                form.renameField("_8", "P09_B");
                form.renameField("_10", "P10_B");
                form.renameField("_11", "P11_B");
                form.renameField("_13", "P12_B");
                form.renameField("_12", "P13_B");
                form.renameField("_14", "P14_B");
                form.renameField("_15", "P15_B");

                stamper.close();
                reader.close();
            }
        }
        
        double t = (System.currentTimeMillis() - time)/1000;
        JOptionPane.showMessageDialog(null,"Proceso finalizado (" +  String.format("%.1f", t) + " segs).");        
    }
    
    public void getEntregaHonor(String dir, String curso, String periodo) throws IOException {  
        
        //Get the PEC files of dir
        File folder = new File(dir);
        FilenameFilter pdfFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                if (lowercaseName.endsWith(".pdf")) {
                    return true;
                } else {
                    return false;
                }
            }
        };
        File[] listOfFiles = folder.listFiles(pdfFilter);
        
        //Loop thorugh the PEC files
        boolean lProblems = false;
        List<String> lines = new ArrayList<String>();
        List<String> problems = new ArrayList<String>();
        lines.add("DNI;Curso;Periodo;entregada;honor");
        for (File file : listOfFiles) {
            if (file.isFile()) {                
                String n = file.getName();
                String dni = n.substring(n.lastIndexOf("_")+1,n.lastIndexOf(".pdf"));      //student's dni
                String c = dni.trim() + ";" + curso + ";'" + periodo + "';" + "1";           //the student has PEC
            
                boolean honor = false;
                PdfReader reader = new PdfReader(file.getAbsolutePath());
                AcroFields form = reader.getAcroFields();
                if (form.getFields().size()>0) {
                    honor = (form.getField("HONOR").equalsIgnoreCase("yes"));   //get honor field
                }
                else {
                    lProblems = true;
                    problems.add(dni);      //the pdf is not readable
                }
                
                //add honor information to data
                c = c + ";" + ((honor) ? "1" : "0");
                lines.add(c);
            }

        }
        //write pec data file
        Path fdata = Paths.get(dir + "/honor_entrega.txt");
        Files.write(fdata, lines, Charset.forName("UTF-8"));
        //write problems file
        if (lProblems) {
            Path fproblems = Paths.get(dir + "/problemas.txt");
            Files.write(fproblems, problems, Charset.forName("UTF-8"));
        }
        
        if (lProblems) {
            JOptionPane.showMessageDialog(null,"Proceso finalizado.\nHay problemas.");
        }
        else {
            JOptionPane.showMessageDialog(null,"Proceso finalizado");
        }
    }
    
    public void getEntregaHonorPEC1(String dir) throws IOException {  
        
        //Get the folders of the original directory dir
        File orig = new File(dir);
        String[] directories = orig.list(new FilenameFilter() {
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        });
        
        //Loop thorugh the folders
        boolean lProblems = false;
        List<String> lines = new ArrayList<String>();
        List<String> problems = new ArrayList<String>();
        lines.add("DNI;entregada;mdb;pdf;honor");
        for (String f : directories) {
            String dni = f.substring(f.lastIndexOf("_")+1);     //student's dni
            String c = dni + ";1";                       //the student has PEC1
            
            //Get list of files for the student and confirm PEC1 elements
            boolean foundMdb = false;
            boolean foundPdf = false;
            boolean honor = false;
            File folder = new File(dir + "/" + f);
            File[] listOfFiles = folder.listFiles();
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    String n = listOfFiles[i].getName();
                    String ext = n.substring(n.lastIndexOf(".")+1);     //file extension
                    
                    //there's a database
                    if (ext.equals("mdb") || ext.equals("accdb")) {
                        foundMdb = true;
                    }
                    
                    //there's a pdf form file
                    if (ext.equals("pdf")) {
                        foundPdf = true;
                        //open pdf file
                        String pdf = listOfFiles[i].getAbsolutePath();
                        PdfReader reader = new PdfReader(pdf);
                        AcroFields form = reader.getAcroFields();
                        if (form.getFields().size()>0) {
                            //get honor field
                            honor = (form.getField("HONOR").equalsIgnoreCase("yes"));
                        }
                        else {
                            //the pdf is not readable
                            lProblems = true;
                            problems.add(dni);
                        }
                    }
                }
            }

            //add mdb, pdf, honor information to data
            c = c + ";" + ((foundMdb) ? "1" : "0");
            c = c + ";" + ((foundPdf) ? "1" : "0");
            c = c + ";" + ((honor) ? "1" : "0");
            lines.add(c);
        }
        //write pec1 data file
        Path fdata = Paths.get(dir + "/datos_pecs.txt");
        Files.write(fdata, lines, Charset.forName("UTF-8"));
        //write problems file
        if (lProblems) {
            Path fproblems = Paths.get(dir + "/problemas.txt");
            Files.write(fproblems, problems, Charset.forName("UTF-8"));
        }
        
        if (lProblems) {
            JOptionPane.showMessageDialog(null,"Proceso finalizado.\nHay problemas.");
        }
        else {
            JOptionPane.showMessageDialog(null,"Proceso finalizado");
        }
    }

    public void test(String dir) throws IOException {
        File folder = new File(dir);
        FilenameFilter pdfFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                if (lowercaseName.endsWith(".pdf")) {
                    return true;
                } else {
                    return false;
                }
            }
        };
        File[] listOfFiles = folder.listFiles(pdfFilter);

        for (File file : listOfFiles) {
            if (file.isFile()) {                
                String n = file.getName();
                String dni = n.substring(n.lastIndexOf("_")+1,n.lastIndexOf(".pdf"));

                System.out.println(dni);
                PdfReader reader = new PdfReader(file.getAbsolutePath());
                AcroFields form = reader.getAcroFields();
                
                for (String key : form.getFields().keySet()) {
                    System.out.println(key);
                }
            }
        }
    }
}
