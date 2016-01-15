import com.cybozu.labs.langdetect.LangDetectException;
import org.javatuples.Pair;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * Created by Sermilion on 29/06/15.
 */
public class main {
    private static boolean TRAIN = false;
    static Database db = new Database();

    public static void addWords(String dir, Trainer tr, String lang){
        final File folder = new File(dir);
        ArrayList<String> articlesForTag;
        try {
            articlesForTag =  getArticlesForTagFromFolder(folder);
            ArrayList<String[]> termsForTag = tr.tokenizeArticles(articlesForTag);
            articlesForTag = null;
            ArrayList<Pair> allTermsForArticlesForTagNoStopwords = tr.removeStopwords(termsForTag);
            termsForTag = null;
            db.addWords(allTermsForArticlesForTagNoStopwords, "tag_temp");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]){
        String sessionLang = "en";
        ArrayList<String >allTerms = db.getAllTerms(sessionLang);
        HashMap<Integer, double[]> db_centroids = db.getAllCentroids();

        LangDetector ld = null;
        try {
            ld = new LangDetector();

            if(TRAIN){
                db.copyWordsFromTemp(sessionLang);
                String[] tags = new String[]{"tech","sports", "world","programming","ios", "android", "microsoft","apple","google","fashion", "music"};
                int count=0;
                for(String tag:tags) {
                    int lastId = 0;
                    int tagIdCount = db.getIdsCountForTag(tag);
                    int limit = 100;
                    int times = tagIdCount / limit + ((tagIdCount % limit > 0) ? 1 : 0);
                    String lang = sessionLang;
                    for (int i = 0; i < 2; i++) {
                        Trainer new_tag_trainer = new Trainer(tag, limit, lastId, allTerms, lang, db, ld);
                        lastId = new_tag_trainer.getLastId();
                        System.out.println("Centroid length: " + new_tag_trainer.getCentroid().length);
                        new_tag_trainer = null;
                    }
                    count++;
                    System.out.println("Training " + count + " is finished!");

                }




//                db.copyWordsFromTemp(sessionLang);
//                Trainer tr = new Trainer(ld);
////                String[] trainingFolds = {"alum","coconut","coffee","cpu","fuel","housing","lead","potato","rice","sugar","tea","wheat"};
//                String[] trainingFolds = {"tech","politics","world","android","programming"};
//
//                for(String label:trainingFolds){
//                    addWords(label, tr, sessionLang);
//                }
//                for(String label:trainingFolds){
//                    Trainer new_tag_trainer = null;
//                    new_tag_trainer = new Trainer(label, allTerms, db, ld);
//                    System.out.println("Centroid length: "+new_tag_trainer.getCentroid().length);
//                    new_tag_trainer=null;
//                }
            }else{
                final File folder = new File("alum_test");
//                ArrayList<String> testArticlesAndIds = getArticlesForTagFromFolder(folder);


                int classifyTrue = 0;
            ArrayList<Pair<Integer, String>> testArticlesAndIds = db.getArticlesAndIdsForTag(1107, 20,"en", 0);
                for(int i =0; i<testArticlesAndIds.size();i++) {
                    String article = testArticlesAndIds.get(i).getValue1();
                    if (article.length()>100) {
                        Trainer input_trainer = new Trainer(article, sessionLang, allTerms, db, ld);
                        HashMap<Integer, double[]> centroids = db_centroids;
                        Map<Integer, Double> tagCos = new HashMap<>();

                        for (Map.Entry<Integer, double[]> entry : centroids.entrySet()) {
//                            for(int j=0;j<entry.getValue().length;j++){
//                                if(entry.getValue()[j]>0){
//                                    System.out.print(entry.getValue()[j] + ", ");
//
//                                }
//                            }
//                            System.out.println();
                            double cos = cosineSimilarity(entry.getValue(), input_trainer.getCentroid());
                            tagCos.put(entry.getKey(), cos);
                        }

                        boolean ASC = true;
                        boolean DESC = false;
                        SortMapByValue sorter = new SortMapByValue();
                        Map<Integer, Double> sortedMapAsc = sorter.sortByComparator(tagCos, DESC);

                        Set<Map.Entry<Integer, double[]>> entries = db_centroids.entrySet();
                        ArrayList<Integer> tags = new ArrayList<Integer>();
                        for (Map.Entry<Integer, double[]> e : entries) {
                            tags.add(e.getKey());
                        }
                        sorter.printMap(sortedMapAsc);
                        System.out.println(article);
                        System.out.println("########## -----------------");
                    }else{
                        System.out.println("Article is too short!");
                    }
                }
            }
        } catch (LangDetectException e) {
            e.printStackTrace();
        }
    }
    public static double cosineSimilarity(double[] vectorA, double[] vectorB) {
        for(int i=0;i<vectorA.length;i++){
            if(vectorA[i]>0){
//                System.out.print("cosineSimilarity A: "+vectorA[i] +", ");
            }else{
//                System.out.println("0");
            }
        }
//        System.out.println("AAAAAAAAAAAAAAAAAAA");
//        for(int i=0;i<vectorB.length;i++){
//            if(vectorB[i]>0){
//                System.out.println("cosineSimilarity B: "+vectorB[i]  +"index: "+i);
//            }else{
////                System.out.println("00");
//            }
//        }
//        System.out.println("BBBBBBBBBBBBBBBBBBBBBBB");
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        int times = 0;
        if(vectorA.length>vectorB.length){
            times = vectorB.length;
        }else{
            times = vectorA.length;
        }
        double a = 0;
        for (int i = 0; i < times; i++) {
//            if(vectorA[i]>0 && vectorB[i]>0){
//                System.out.println("cosSim- a: "+vectorA[i]+" index:"+i+". b: "+vectorB[i] );
//                a = vectorA[i] * vectorB[i];
//                System.out.println("cosSim- a: "+vectorA[i]+" index:"+i+". b: "+vectorB[i] );
//            }else if(vectorA[i]>0 && vectorB[i]>0){
//                System.out.println("cosSim- a: "+vectorA[i]+" index:"+i+". b: "+vectorB[i] );
//            }
            a = vectorA[i] * vectorB[i];
            dotProduct += a;
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        double cos = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
        return cos;
    }



    public static ArrayList<String> getArticlesForTagFromFolder(final File folder) throws FileNotFoundException{
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
