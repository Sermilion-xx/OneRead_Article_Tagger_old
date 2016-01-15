/**
 * Created by Sermilion on 29/06/15.
 */

import org.javatuples.Pair;
import org.postgresql.util.PSQLException;

import java.sql.*;
import java.util.*;

public class Database {

    private Connection connection;

    public Database(){
        if(this.connection==null) {
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                System.out.println("Where is your PostgreSQL JDBC Driver? "
                        + "Include in your library path!");
                e.printStackTrace();
                return;
            }

            try {
                this.connection = DriverManager.getConnection(
                        "jdbc:postgresql://oneread.io:5432/oneread", "rbatukaev",
                        "22AunaledE25!");
            } catch (SQLException e) {
                System.out.println("Connection Failed! Check output console");
                e.printStackTrace();
                return;
            }
            if (this.connection != null) {
                System.out.println("You made it, take control your database now!");
            } else {
                System.out.println("Failed to make connection!");
            }
        }
    }

    public void closeConnection(){
        try {
            this.connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> getAllTerms(String lang){
        ArrayList<String> allTerms = new ArrayList<String>();
        try{
            Statement st = connection.createStatement();
            ResultSet rs = null;
            if(lang.equals("en"))
                rs = st.executeQuery("SELECT * FROM tag WHERE lang ='"+lang+"'");
            else
                rs = st.executeQuery("SELECT * FROM tag");
            int index = 0;
            while (rs.next())
            {
                String term = rs.getString(2).replaceAll("[\\d#€¡¢∞§•ªº\"≠!@£$%^&*()_+=,.`~;:<>'\\|{}/]", "").replaceAll("-"," ").replaceAll(" ","");
                if(term.length()>0 ) {
                    allTerms.add(index, term);
                    index++;
                }
            }
            Collections.sort(allTerms);
            rs.close();
            st.close();
        }catch(SQLException e){
            System.out.println("SQL exception was raised while performing SELECT: "+e);
        }
        return allTerms;
    }

    public ArrayList<String> getAllTempTerms(String lang){
        ArrayList<String> allTerms = new ArrayList<String>();
        try{
            Statement st = connection.createStatement();
            ResultSet rs = null;
            if(lang.equals("en"))
                rs = st.executeQuery("SELECT * FROM tag_temp WHERE lang ='"+lang+"'");
            else
                rs = st.executeQuery("SELECT * FROM tag_temp");
            int index = 0;
            while (rs.next())
            {
                String term = rs.getString(2).replaceAll("[\\d#€¡¢∞§•ªº\"≠!@£$%^&*()_+=,.`~;:<>'\\|{}/]", "").replaceAll("-"," ").replaceAll(" ","");
                if(term.length()>0 ) {
                    allTerms.add(index, term);
                    index++;
                }
            }
            Collections.sort(allTerms);
            rs.close();
            st.close();
        }catch(SQLException e){
            System.out.println("SQL exception was raised while performing SELECT: "+e);
        }
        return allTerms;
    }

    public HashMap<Integer,String> getAllTermsAndIds(){
        HashMap<Integer,String> allTerms = new HashMap<>();
        try{
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM tag");
            while (rs.next())
            {
                allTerms.put(rs.getInt(1), rs.getString(2));
            }
            rs.close();
            st.close();
        }catch(SQLException e){
            System.out.println("SQL exception was raised while performing SELECT: "+e);
        }
        return allTerms;
    }

    public int getIdByTag(String tag){
        try{
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(
                    "SELECT id FROM tag WHERE name='"+tag+"'");
            if (rs.next())
            {
                int id = rs.getInt(1);
                return id;

            } rs.close();
            st.close();
        }catch(SQLException e){
            System.out.println("SQL exception was raised while performing SELECT: "+e);
        }
        return -1;
    }


    public Pair<ArrayList, Integer> getArticlesForTag(int tag_id, int limit, String lang, int lastId){
        ArrayList result = new ArrayList<String>();
        Pair<ArrayList, Integer> res = null;
        int localLastId=-1;
        try{
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(
                    "SELECT * FROM article a INNER JOIN article_tags att ON a.id = att.article_id INNER JOIN blog b ON b.id=a.blog_id" +
                    " WHERE att.tag_id="+ tag_id +" AND a.id>"+lastId+" AND b.lang='"+lang+"' LIMIT "+limit+""
            );
            while (rs.next())
            {
                String article = rs.getString(3);
                if(localLastId==-1){
                    localLastId=rs.getInt(1);
                }
                result.add(article);

            }
            res = new Pair<>(result, localLastId);

            rs.close();
            st.close();
        }catch(SQLException e){
            System.out.println("SQL exception was raised while performing SELECT: "+e);
        }
        return res;
    }

    public ArrayList<Pair<Integer, String>> getArticlesAndIdsForTag(int tag_id, int limit, String lang, int lastId){

        ArrayList result = new ArrayList<Pair<Integer, String>>();
        Pair<Integer, String> article = null;
        try{
            Statement st = connection.createStatement();

            ResultSet rs = st.executeQuery(
                    "SELECT * FROM article a INNER JOIN article_tags att ON a.id = att.article_id INNER JOIN blog b ON b.id=a.blog_id" +
                            " WHERE att.tag_id="+ tag_id +" AND a.id>"+lastId+" AND b.lang='"+lang+"' LIMIT "+limit+""
            );
            while (rs.next())
            {
                String desc = rs.getString(3);
                int id = rs.getInt(1);
                article = new Pair<>(id,desc);
                result.add(article);
            }
            rs.close();
            st.close();
        }catch(SQLException e){
            System.out.println("SQL exception was raised while performing SELECT: "+e);
        }
        return result;
    }

    /**
     * @param tag for which to get number of articles
     * @return total count of articles for that tag
     */
    public int getIdsCountForTag(String tag){
        int count = 0;
        try {
            int tag_id = this.getIdByTag(tag);
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(
                    "SELECT count(id) as count FROM article a INNER JOIN article_tags att ON a.id = att.article_id" +
                            " WHERE att.tag_id=" + tag_id
            );
            while (rs.next()) {
                count = rs.getInt("count");
            }
        }catch(SQLException e){
        System.out.println("SQL exception was raised while performing SELECT: "+e);
        }
        return count;
    }

    public ArrayList<String> getArticlesFromBlogByTag(int blog, int tag_id){
        ArrayList<String> result = new ArrayList<String>();
        try{
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(
                    "SELECT * FROM article a INNER JOIN article_tags t ON a.id = t.article_id WHERE blog_id = '"+blog+"' AND t.tag_id="+tag_id+"");
            while (rs.next())
            {
                String article = rs.getString(3);
                result.add(article);

            }
            rs.close();
            st.close();
        }catch(SQLException e){
            System.out.println("SQL exception was raised while performing SELECT: "+e);
        }
        return result;
    }


    public int addTag(String tag){
        ResultSet rs = null;
        int key = -1;
        ArrayList ids = new ArrayList<String>();
        try{
                Statement st = connection.createStatement();
                st.executeUpdate("INSERT INTO tag (name) SELECT '"+tag+"' WHERE NOT EXISTS (SELECT '"+tag+"' FROM tag WHERE name = '"+tag+"')");
            rs = st.getGeneratedKeys();
            if ( rs.next() ) {
                // Retrieve the auto generated key(s).
                key = rs.getInt(1);
            }
            if(key==-1){
                key = this.getIdByTag(tag);
            }

        }catch (SQLException e){
            System.out.println("SQL exception was raised while performing INSERT: "+e);
        }
        return key;
    }







    public ArrayList<Integer> addWords(ArrayList allTermsForTag, String table, String...lang1){
        ResultSet rs = null;
        ArrayList<Integer> ids = new ArrayList();
        try{
            PreparedStatement stmt = connection.prepareStatement("WITH new_values (name, lang) as (values (?,?)), " +
                    "upsert as(update "+table+" m set name = nv.name, lang = nv.lang FROM new_values nv WHERE m.name = nv.name RETURNING m.*) " +
                    "INSERT INTO "+table+" (name, lang)SELECT name, lang FROM new_values WHERE NOT EXISTS " +
                    "(SELECT 1 FROM upsert up WHERE up.name = new_values.name)");
            if(allTermsForTag.get(0).getClass().getName().equals("org.javatuples.Pair")){
                for(Pair<ArrayList<String>, String> pair: (ArrayList<Pair>)allTermsForTag) {
                    String lang = pair.getValue1();
                    for(String term: pair.getValue0()) {
                        stmt.setString(1, term.toLowerCase());
                        stmt.setString(2, lang.toLowerCase());
                        stmt.addBatch();
                    }
                }
                stmt.executeBatch();
            }else{
                for(String term: (ArrayList<String>)allTermsForTag) {
                        stmt.setString(1, term.toLowerCase());
                        stmt.setString(2, lang1[0].toLowerCase());
                        stmt.addBatch();
                }
                stmt.executeBatch();

            }
//            rs = stmt.getGeneratedKeys();
//            while (rs.next()) {
//                int id = rs.getInt(1);
//                ids.add(id);
//            }
            stmt.close();

        }catch (SQLException e){
            System.out.println("SQL exception was raised while performing batch INSERT: "+e.getNextException());

        }
        return ids;
    }

    public ArrayList<Integer> addTempWords(ArrayList<Pair> allTermsForTag){
        ResultSet rs = null;
        ArrayList<Integer> ids = new ArrayList<Integer>();
        try{
            PreparedStatement stmt = connection.prepareStatement("WITH new_values (name, lang) as (values (?,?)), " +
                    "upsert as(update tag_temp m set name = nv.name, lang = nv.lang FROM new_values nv WHERE m.name = nv.name RETURNING m.*) " +
                    "INSERT INTO tag_temp (name, lang)SELECT name, lang FROM new_values WHERE NOT EXISTS " +
                    "(SELECT 1 FROM upsert up WHERE up.name = new_values.name)");
            for(Pair<ArrayList<String>, String> pair: allTermsForTag) {
                String lang = pair.getValue1();
                for(String term: pair.getValue0()) {
                    stmt.setString(1, term.toLowerCase());
                    stmt.setString(2, lang.toLowerCase());
                    stmt.addBatch();
                }
            }
            stmt.executeBatch();
            rs = stmt.getGeneratedKeys();
            while (rs.next()) {
                int id = rs.getInt(1);
                ids.add(id);
            }
            stmt.close();
        }catch (SQLException e){
            System.out.println("SQL exception was raised while performing INSERT addTempWords: "+e.getNextException());
        }
        return ids;
    }

    public void copyWordsFromTemp(String lang){
        try {
            connection.setAutoCommit(false);
            ArrayList<String> tempTerms = this.getAllTempTerms(lang);
            if(tempTerms.size()>0){
                this.addWords(tempTerms, "tag",lang);
                PreparedStatement stmt1 = connection.prepareStatement("DELETE FROM tag_temp;");
                PreparedStatement stmt2 = connection.prepareStatement("DELETE FROM tag_centroid;");
                stmt1.executeUpdate();
                stmt2.executeUpdate();
                stmt1.close();
                stmt2.close();
            }
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                connection.rollback();
                System.out.println("JDBC Transaction rolled back successfully");
            } catch (SQLException e1) {
                System.out.println("SQLException in rollback"+e.getMessage());
            }
        }
    }

    public void saveCentroid(String centroid, int mainTagId, boolean isMainTag, int divisor, String lang){
        try{
            Statement st = connection.createStatement();
            int main=0;
            if(isMainTag){
                main=1;
            }
            st.executeUpdate("INSERT INTO tag_centroid (tag, centroid, main, divisor, lang) VALUES ("+mainTagId+",'"+centroid+"', "+main+", "+divisor+", '"+lang+"');");
        }catch (SQLException e){
            System.out.println("SQL exception INSERT - saveCentroid: "+e);
        }
    }

//    public double[] getCentroid(int tag){
//        String centroidString = "";
//        double[] results = null;
//        try{
//            Statement st = connection.createStatement();
//            ResultSet rs = st.executeQuery("SELECT centroid FROM tag_centroid WHERE tag ='"+tag+"'");
//            while (rs.next())
//            {
//                centroidString = rs.getString(1);
//            }
//            String[] temp = centroidString.split(",");
//            results = new double[temp.length];
//
//            for (int i = 0; i < temp.length; i++) {
//                try {
//                    results[i] = Double.parseDouble(temp[i]);
//                } catch (NumberFormatException nfe) {
//
//                }
//            }
//
//            rs.close();
//            st.close();
//            return results;
//        }catch(SQLException e){
//            System.out.println("SQL exception was raised while performing SELECT: "+e);
//        }
//        return new double[1];
//    }

    public Pair<Integer, double[]> getCentroidAndDiv(int tag){
        String centroidString = "";
        double[] results = null;
        int divisor = 0;
        try{
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery("SELECT centroid, divisor FROM tag_centroid WHERE tag ='"+tag+"'");
            while (rs.next())
            {
                centroidString = rs.getString(1);
                if(divisor!=-1)
                    divisor = rs.getInt(2);

            }
            String[] temp = centroidString.split(",");
            results = new double[temp.length];

            for (int i = 0; i < temp.length; i++) {
                try {
                    results[i] = Double.parseDouble(temp[i]);
                } catch (NumberFormatException nfe) {

                }
            }
            Pair<Integer, double[]> res = new Pair<>(divisor, results);
            rs.close();
            st.close();
            return res;
        }catch(SQLException e){
            System.out.println("SQL exception: SELECT in method getCentroidAndDiv: "+e);
        }
        return new Pair<>(0,new double[0]);
    }

    public HashMap<Integer,double[]> getAllCentroids(){
        HashMap<Integer,double[]> centroids = new HashMap<>();

        try{
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM tag_centroid");
            while (rs.next())
            {
                int tag = rs.getInt("tag");
                String centroid = rs.getString("centroid");
                String[] temp = centroid.split(",");
                double[] results = new double[temp.length];

                for (int i = 0; i < temp.length; i++) {
                    try {
                        results[i] = Double.parseDouble(temp[i]);
                    } catch (NumberFormatException nfe) {

                    }
                }
                centroids.put(tag, results);
            }
            rs.close();
            st.close();
            return centroids;
        }catch(SQLException e){
            System.out.println("SQL exception was raised while performing SELECT: "+e);
        }
        return null;
    }

//    private String ArrayToString(String[] array)
//    {
//        StringBuilder buffer = new StringBuilder();
//
//        buffer.append("'").append(array[0]).append("'");
//
//        for (int i = 1; i < array.length; i++)
//        {
//            buffer.append(",");
//            buffer.append("'").append(array[i]).append("'");
//        }
//
//        return buffer.toString();
//    }
//    public ArrayList<Integer> getAllArticleTagsIds(){
//        ArrayList<Integer> articleIds = new ArrayList<Integer>();
//        try{
//            Statement st = connection.createStatement();
//            ResultSet rs = st.executeQuery("SELECT DISTINCT tag_id FROM article_tags");
//            while (rs.next())
//            {
//                int id = rs.getInt(1);
//                articleIds.add(id);
//            }
//            rs.close();
//            st.close();
//            return articleIds;
//        }catch(SQLException e){
//            System.out.println("SQL exception was raised while performing SELECT: "+e);
//        }
//        return null;
//    }

    //    public void addWordsV2(ArrayList<String[]> allTermsForTag){
//        try{
//            Statement st = connection.createStatement();
//            for (String[] articleTerms: allTermsForTag) {
//                for(String term: articleTerms) {
//                    String query = "INSERT INTO tag (name) SELECT '"+term+"' WHERE NOT EXISTS (SELECT '"+term+"' FROM tag WHERE name = '"+term+"')";
//                    st.addBatch(query);
//                }
//                st.executeBatch();
//            }
//
//            st.close();
//        }catch(SQLException e){
//            System.out.println("SQL exception was raised while performing SELECT: "+e);
//        }
//    }
//
//    public void addWordsV3(ArrayList<String[]> allTermsForTag){
//        ArrayList<Integer> ids = new ArrayList<Integer>();
//        try{
//            ResultSet rs = null;
//            Statement st = connection.createStatement();
//
//            StringBuilder values = new StringBuilder();
//            for (String[] articleTerms: allTermsForTag) {
//                for(String term: articleTerms) {
//                    values.append("('" + term + "'),");
//                }
//            }
//            String sql = "with data(first_name, last_name, uid)  as (values ( 'John', 'Doe', '3sldkjfksjd'),( 'Jane', 'Doe', 'adslkejkdsjfds')) " +
//                    "insert into users (first_name, last_name, uid) select d.first_name, d.last_name, d.uid from data d " +
//                    "where not exists (select 1 from users u2 where u2.uid = d.uid";
//
//            values.setCharAt(values.length()-1, ';');
//            String query = "INSERT INTO tag (name) VALUES "+values.toString()+" ON CONFLICT (name) UPDATE SET name = EXCLUDED.name";
//            st.executeQuery(query);
//            rs = st.getGeneratedKeys();
//            while (rs.next()) {
//                    int id = rs.getInt(1);
//                    ids.add(id);
//                }
//
//            st.close();
//        }catch(SQLException e){
//            System.out.println("SQL exception was raised while performing SELECT: "+e);
//        }
//    }

//    public ArrayList<Integer> getInsertedIds(ArrayList<String[]> termsForTag){
//        ArrayList<Integer> ids = new ArrayList<Integer>();
//        ResultSet rs = null;
//        try{
//            StringBuilder sb = new StringBuilder();
//            for(String[] terms: termsForTag){
//                if(terms.length>0)
//                sb.append(this.ArrayToString(terms)).append(",");
//            }
//            sb.setLength(sb.length() - 1);
//
//            Statement st = connection.createStatement();
//            String query = "SELECT id FROM tag WHERE name IN("+sb.toString()+")";
//            st.executeQuery(query);
//            rs = st.executeQuery(query);
//            while (rs.next())
//            {
//                int id = rs.getInt(1);
//                ids.add(id);
////                        System.out.printf("added id: "+id);
//            }
//            return ids;
//        }catch(SQLException e){
//            System.out.println("SQL exception was raised while performing SELECT: "+e);
//        }
//
//        return null;
//    }

//    public ArrayList<Integer> addWords(ArrayList<String[]> allTermsForTag) {
//        ArrayList ids = new ArrayList<Integer>();
//        ResultSet rs = null;
//        try{
//            Statement st = connection.createStatement();
//            for (String[] articleTerms: allTermsForTag) {
//                for(String term: articleTerms) {
//                    String query = "WITH a AS (INSERT INTO tag (name) SELECT '"+term+"' WHERE NOT EXISTS (SELECT name FROM tag WHERE name = '"+term+"') " +
//                            "RETURNING id) SELECT id FROM a UNION SELECT id FROM tag WHERE name = '"+term+"'";
//                    rs = st.executeQuery(query);
//                    while (rs.next())
//                    {
//                        int id = rs.getInt(1);
//                        ids.add(id);
////                        System.out.printf("added id: "+id);
//
//                    }
//
//                }
//
//            }
//            System.out.println("Done");
//            rs.close();
//            st.close();
//        }catch(SQLException e){
//            System.out.println("SQL exception was raised while performing SELECT: "+e);
//        }
//        return ids;
//
//
//    }

//    public ArrayList<Integer> addWords(ArrayList<String[]> allTermsForTag){
//        ResultSet rs = null;
//        ArrayList ids = new ArrayList<Integer>();
//
//        try{
//            Statement st = connection.createStatement();
//            for (String[] articleTerms: allTermsForTag) {
//                for(String term: articleTerms) {
//                    String query = null;
////                    query = "INSERT INTO tag (name) SELECT '"+term+"' WHERE NOT EXISTS (" +
////                            "SELECT '"+term+"' FROM tag WHERE name = '"+term+"');";
//                            query = "WITH a AS (INSERT INTO tag (name) SELECT '"+term+"' WHERE NOT EXISTS (SELECT name FROM tag WHERE name = '"+term+"') " +
//                            "RETURNING id) SELECT id FROM a UNION SELECT id FROM tag WHERE name = '"+term+"'";
//                    st.addBatch(query);
//                }
//                st.executeBatch();
//                rs = st.getGeneratedKeys();
//                while (rs.next()) {
//                    int id = rs.getInt(1);
//                    ids.add(id);
//                }
//            }
//
//            st.close();
//            return ids;
//        }catch (SQLException e){
//            System.out.println("SQL exception was raised while performing batch INSERT: "+e.getNextException());
//            System.out.println("dub");
//        }
//        return null;
//    }


//    public boolean addTagRelations(int mainTagId, ArrayList<Integer> relatedTagsIds){
//        ResultSet rs = null;
//        ArrayList ids = new ArrayList<Integer>();
//        try{
//            Statement st = connection.createStatement();
//            for(int id: relatedTagsIds) {
//                String query = "INSERT INTO tag_to_tag (tag1_id, tag2_id) SELECT "+mainTagId+", "+id+
//                        " WHERE NOT EXISTS (SELECT tag1_id, tag2_id FROM tag_to_tag WHERE tag1_id ="+mainTagId+" AND tag2_id="+id+" );";
//                st.addBatch(query);
//            }
//            st.executeBatch();
//            st.close();
//            return true;
//        }catch (SQLException e){
//            System.out.println("SQL exception was raised while performing batch INSERT: "+e);
//        }
//        return false;
//    }

//    public boolean addTfw(ArrayList<double[]> tfws, ArrayList<Integer> returnedIds) {
//        ResultSet rs = null;
//        try{
//            Statement st = connection.createStatement();
//            connection.setAutoCommit(false);
//
//            PreparedStatement stmt = connection.prepareStatement("INSERT INTO tag_to_tag (tag1_id, tag2_id) SELECT ? WHERE (" +
//                    "SELECT ? FROM tag_to_tag WHERE tag2_id=?);");
//            int id=0;
//            for(double[] tfwFotArticle: tfws) {
//                    for(double tfw: tfwFotArticle) {
//                        if (tfw>0) {
//                            stmt.setInt(1, returnedIds.get(id));
//                            stmt.setDouble(2, tfw);
//                            stmt.addBatch();
//                        }
//                        id++;
//                }
//            }
//            stmt.executeBatch();
//            connection.commit();
//            st.close();
//            return true;
//        }catch (SQLException e){
//            System.out.println("SQL exception was raised while performing batch INSERT: "+e.getNextException());
//            return false;
//        }
//
//    }

//    public boolean addTfw(ArrayList<double[]> tfws, ArrayList<Integer> returnedIds, int maitTagId) {
//        ResultSet rs = null;
//        try{
//            Statement st = connection.createStatement();
//            int id = 0;
//            for(double[] tfwFotArticle: tfws) {
//                for(int i =0; i<tfwFotArticle.length; i++){
//                    if(tfwFotArticle[i]>0){
//                        String query = "UPDATE tag_to_tag SET tfw = '"+tfwFotArticle[i]+"' WHERE tag1_id='"+maitTagId+"' AND tag2_id = '"+returnedIds.get(id)+"'";
//                        st.addBatch(query);
//                    id++;
//                    }else{
//
//                    }
//                }
//            }
//            st.executeBatch();
//            st.close();
//            return true;
//        }catch (SQLException e){
//            System.out.println("SQL exception was raised while performing batch INSERT: "+e.getNextException());
//            return false;
//        }
//
//    }




}

