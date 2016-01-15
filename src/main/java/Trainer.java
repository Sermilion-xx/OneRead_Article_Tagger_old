/**
 * Created by Sermilion on 29/06/15.
 */

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.lang.Math;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import com.cybozu.labs.langdetect.LangDetectException;
import org.javatuples.Pair;


public class Trainer {
    private Database db;
    public LangDetector ld;
    private ArrayList<String> articlesForTag;
    private ArrayList<double[]> tfwForTermsInArticles;
    private double[] centroid;
    private boolean isMainTag=false;
    private int lastId;
    private int divisor;

    public Trainer(LangDetector ld){
            this.ld = ld;
    }

    //Trainer
    public Trainer(String tag, int rowLimit, int lastId, ArrayList<String > allTerms, String lang, Database db, LangDetector ld){
        this.lastId = lastId;
        this.centroid = null;
        this.db = db;
        this.ld = ld;
        int newTagId = -1;
        //train tag
        newTagId = db.addTag(tag);
        Pair<ArrayList, Integer> articlesAndId = db.getArticlesForTag(newTagId, rowLimit,lang, this.lastId);
        articlesForTag = articlesAndId.getValue0();
        this.lastId = articlesAndId.getValue1();
            train(lang,allTerms);
        this.centroid = centroid(this.tfwForTermsInArticles, newTagId);
            PostgreSQLDoubleArray psqlArray = new PostgreSQLDoubleArray(this.centroid);
            db.saveCentroid(psqlArray.toString(), newTagId, this.isMainTag, this.divisor, lang);
            System.out.println("Done!");
    }

    //Test Trainer
    public Trainer(String dir, ArrayList<String > allTerms, Database db, LangDetector ld) throws FileNotFoundException {
        this.lastId = lastId;
        this.centroid = null;
        this.db = db;
        this.ld = ld;
        int newTagId = -1;
        //train tag
        newTagId = db.addTag(dir);
        final File folder = new File(dir);
        articlesForTag = new ArrayList(); //
        articlesForTag =  getArticlesForTagFromFolder(folder);
        train("en", allTerms);
        this.centroid = centroid(this.tfwForTermsInArticles, newTagId);
        PostgreSQLDoubleArray psqlArray = new PostgreSQLDoubleArray(this.centroid);
        db.saveCentroid(psqlArray.toString(), newTagId, this.isMainTag, this.divisor, "en");
        System.out.println("Done! Centroid length: "+this.centroid.length);
    }

    //classifier
    public Trainer(String article, String lang, ArrayList<String > allTerms, Database db, LangDetector ld) {
        this.centroid = null;
        this.db = db;
        this.ld = ld;
        articlesForTag = new ArrayList<>();
        articlesForTag.add(article.toLowerCase());
        this.isMainTag = true;
        train(lang, allTerms);
        this.centroid = centroid(this.tfwForTermsInArticles, -1);
    }

    private void train(String lang, ArrayList<String > allTerms1){
        ArrayList<String[]> termsForTag = this.tokenizeArticles(articlesForTag);
        articlesForTag= null;
        ArrayList<Pair> allTermsForArticlesForTagNoStopwords = removeStopwords(termsForTag);
        termsForTag=null;
        db.addWords(allTermsForArticlesForTagNoStopwords, "tag_temp", lang);
        ArrayList<String > allTerms = allTerms1;
        this.tfwForTermsInArticles = new ArrayList<double[]>();
        tfwForTag(allTermsForArticlesForTagNoStopwords, allTerms);
        allTermsForArticlesForTagNoStopwords=null;
    }


    public double[] getCentroid() {
        return this.centroid;
    }
    public int getLastId() {
        return lastId;
    }

    public void doNothing(){

    }

    /**
     * Function to tokenize articles and remove punctuations
     */
    public ArrayList<String[]> tokenizeArticles(ArrayList<String> articlesForTag){
        ArrayList<String[]> termsForTag = new ArrayList<String[]>();
        for(String article : articlesForTag){
            String[] tokens = article.replaceAll("[\\d\\r#€¡¢∞§•ªº\"≠!@£$%^&*()_+=,.`~;:<>'\\|{}/›]", "").replaceAll("…"," ").replaceAll("\\n"," ").split(" ");

            List<String> list = new ArrayList<String>(Arrays.asList(tokens));
            list.removeAll(Arrays.asList("", null,"-"));
            List<String> result = list.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
            termsForTag.add(result.toArray(new String[list.size()]));
        }
        return termsForTag;
    }

    //
    public ArrayList<Pair> removeStopwords(ArrayList<String[]> termsForTag) {
        ArrayList<Pair> no_stopwords = new ArrayList<>();
        String[] stopwords;
        try{
            ArrayList<String> list1 = null;
            ArrayList<String> list2 = null;
            for(String[] article: termsForTag){

                    StringBuilder sb = new StringBuilder();
                    for(String s: article){
                        sb.append(s).append(" ");
                    }
                String lang = this.ld.detect(sb.toString());
                    if (lang.equals("ru")) {
                        stopwords = readLines("russian_stopwords.txt");
                    } else {
                        stopwords = readLines("english_stopwords.txt");
                    }
                    sb = null;
                list1 = new ArrayList<>(Arrays.asList(article));
                list2 = new ArrayList<>(Arrays.asList(stopwords));
                list1.removeAll(list2);
                Pair<ArrayList, String> articleLang = new Pair<>(list1, "en");
                no_stopwords.add(articleLang);

            }
            return no_stopwords;
        }catch (IOException e){
            System.out.println("IO Exception: "+e);
        }catch (LangDetectException e){
            System.out.println("Error initializing language detector: "+e);
        }
        return no_stopwords;
    }

    //to read file to array
    public static String[] readLines(String filename) throws IOException {
        FileReader fileReader = new FileReader(filename);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        List<String> lines = new ArrayList<String>();
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            lines.add(line);
        }
        bufferedReader.close();
        fileReader.close();
        return lines.toArray(new String[lines.size()]);
    }

    public ArrayList<double[]> tfwForTag(ArrayList<Pair> allTermsForArticlesForTag, ArrayList<String> allTerms){

        ArrayList<double[]> tfwForTermsInArticles = new ArrayList<double[]>();
        for (Pair<ArrayList, String> termsForArticle: allTermsForArticlesForTag){
            double[] tfw_for_article = new double[allTerms.size()];
            for(int i=0; i<allTerms.size();i++){
                String tempTerm = allTerms.get(i);
                int occurrences = Collections.frequency(termsForArticle.getValue0(), tempTerm);
                if(occurrences==0){
                    tfw_for_article[i]= 0;
                }else{
                    tfw_for_article[i] = 1 + Math.log10(occurrences);
//                    System.out.println("tfw: "+tfw_for_article[i] );
                }
            }
            this.tfwForTermsInArticles.add(tfw_for_article);
        }
//        System.out.println("tfw done -------------------------------------------------------------------------------");
        return tfwForTermsInArticles;
    }

    public double[] centroid(ArrayList<double[]> tfws, int tagId){
        double[][] tfw_matrix = new double[tfws.size()][tfws.get(0).length];
        for(int i=0;i<tfws.size();i++){
                tfw_matrix[i]=tfws.get(i);
        }
        double[] centroid =  getCentroid_v2(tfw_matrix, tagId);
        return centroid;
    }


    private double[] getCentroid_v2(double[][] tfw_matrix, int tagId){
        double[] centroid = new double[tfw_matrix[0].length];

        for (int i = 0; i < tfw_matrix[0].length; i++) {
            double sum = 0;
            double element = 0;
            for (int j = 0; j < tfw_matrix.length; j++) {
                sum += tfw_matrix[j][i];
            }
            element = sum;
            centroid[i] = element;
        }

        Pair<Integer, double[]> oldCentroid = null;
        double[] oldSemiCentroid = null;

        int oldDivisor = 0;
        if(tagId>-1){
            //getting old centroid
            oldCentroid = this.db.getCentroidAndDiv(tagId);
            //creating array for storing semiCentroid
            oldSemiCentroid = new double[oldCentroid.getValue1().length];
            //finding divisor
            oldDivisor = oldCentroid.getValue0();

            //calculating old semi centroid
            if(oldCentroid.getValue1().length>2){
                DoubleStream ds = Arrays.stream(oldCentroid.getValue1());
                final Pair<Integer, double[]> finalOldCentroid = oldCentroid;
                oldSemiCentroid = ds.map(n -> n * finalOldCentroid.getValue0()).toArray();
            }
        }
//        for(int i=0;i<oldSemiCentroid.length;i++){
//            if(oldSemiCentroid[i]>0){
//                System.out.print("Old SemiCentroid: "+oldSemiCentroid[i]+", ");
//            }
//        }

        this.divisor = tfw_matrix.length+oldDivisor;

        double[] newSemiCentroid = new double[centroid.length];
        double[] newCentroid = new double[centroid.length];
        if(oldSemiCentroid!=null && oldSemiCentroid.length>1){
            if(newSemiCentroid.length>oldSemiCentroid.length){
                double[] tempArrsy = new double[newSemiCentroid.length - oldSemiCentroid.length];
                double[] newArray = new double[tempArrsy.length + oldSemiCentroid.length];
                // copy first half
                System.arraycopy(oldSemiCentroid, 0, newArray, 0, oldSemiCentroid.length);
                // copy second half
                System.arraycopy(tempArrsy, 0, newArray, oldSemiCentroid.length, tempArrsy.length);

                final double[] finalOldSemiCentroid = newArray;
                Arrays.setAll(newSemiCentroid, i -> ((finalOldSemiCentroid[i] + centroid[i])/this.divisor));
            }else{
                final double[] finalOldSemiCentroid = oldSemiCentroid;

                Arrays.setAll(newSemiCentroid, i -> ((finalOldSemiCentroid[i] + centroid[i])/this.divisor));

                newCentroid = newSemiCentroid;
                for(int i=0; i<newCentroid.length; i++){
                    if(newCentroid[i]>0){
                        System.out.print(newCentroid[i]+", ");
                    }
                }
            }

        }else{
            for(int i=0; i<centroid.length; i++){
                if(centroid[i]/this.divisor>0){
                    newCentroid[i] = centroid[i]/this.divisor;
                    System.out.print(centroid[i]/this.divisor+", ");
                }
            }
        }
        System.out.println("centroid is done ------------------------------------------------------------------------------");
        return newCentroid;
    }

    public void print2DDoubleArray(double[][] tfw_matrix){
        for(int i=0;i<tfw_matrix[0].length;i++) {
            for (int j = 0; j < tfw_matrix.length; j++) {
                if(tfw_matrix[i][j]>0){
                    System.out.println(tfw_matrix[i][j] + ", " );
                }
            }
            System.out.println();
        }
    }


        public ArrayList<String> getArticlesForTagFromFolder(final File folder) throws FileNotFoundException{
            ArrayList<String> articlesForTag = new ArrayList<>();
        try {
            for (final File fileEntry : folder.listFiles()) {
                if (fileEntry.isDirectory()) {
                    getArticlesForTagFromFolder(fileEntry);
                } else {
                    String content = new Scanner(fileEntry).useDelimiter("\\Z").next();
                    articlesForTag.add(content);
                }
            }
        } catch (IOException e){
            System.out.println("IOEcxeption: "+e);
        }
            return articlesForTag;
    }

}
