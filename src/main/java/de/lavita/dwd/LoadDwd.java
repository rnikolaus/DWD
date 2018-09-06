package de.lavita.dwd;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *
 * @author nik
 */
public class LoadDwd {
    static SimpleDateFormat parseFormat = new SimpleDateFormat("yyyyMMdd");
    static SimpleDateFormat outputformat = new SimpleDateFormat("dd.MM.yyyy");
    /**
     * This describes the position of fields in the fixed width text file
     */
    enum FilePosition{
        STAT(0,5),
        DATE(5,14),
        QN(14,17),
        TG(17,24),
        TN(24,31),
        TM(31,39),
        TX(39,45),
        RFM(45,53),
        FM(53,59),
        FX(59,66),
        SO(66,73),
        NM(73,81),
        RR(81,87),
        PM(87,95);
        private final int startPos;
        private final int endPos;

        private FilePosition(int startPos,int endPos) {
            this.startPos = startPos;
            this.endPos = endPos;
        }
        /**
         * get the field that corresponds to the column (or pos)
         * @param pos
         * @return FilePosition, which is the field of the file
         */
        static FilePosition getPosition(int pos){
            for (FilePosition field: FilePosition.values()){
                if (pos >=field.startPos && pos<field.endPos){ //there are probably faster ways of doing this, but this is fast enough
                    return field;
                }
            }
            throw new RuntimeException("position out of range: "+pos);
        } 
    }
    /**
     * A single result line
     */
    public static class  DataBean{
        private final Map<FilePosition,StringBuilder> result;
        public DataBean(String line) {
            result= new HashMap<>();
            for (int pos =0;pos<line.length();pos++){
                        FilePosition fp = FilePosition.getPosition(pos);
                        if (!result.containsKey(fp)){
                            result.put(fp, new StringBuilder());
                        }
                        result.get(fp).append(line.charAt(pos));
                    }
        }

        /**
         * @return the stat
         */
        public Long getStat() {
            return Long.parseLong(result.get(FilePosition.STAT).toString());
        }

        /**
         * @return the date
         */
        public Date getDate(){
            try {
                return parseFormat.parse(result.get(FilePosition.DATE).toString().trim());
            } catch (ParseException ex) {
                throw new RuntimeException(ex);
            }
        }

        /**
         * @return the qn
         */
        public Double getQn() {
            return getDouble(FilePosition.QN);
        }

        /**
         * @return the tg
         */
        public Double getTg() {
            return getDouble(FilePosition.TG);
        }

        /**
         * @return the tn
         */
        public Double getTn() {
            return getDouble(FilePosition.TN);
        }

        /**
         * @return the tm
         */
        public Double getTm() {
            return getDouble(FilePosition.TM);
        }

        /**
         * @return the tx
         */
        public Double getTx() {
            return getDouble(FilePosition.TX);
        }

        /**
         * @return the rfm
         */
        public Double getRfm() {
            return getDouble(FilePosition.RFM);
        }

        /**
         * @return the fm
         */
        public Double getFm() {
            return getDouble(FilePosition.FM);
        }

        /**
         * @return the fx
         */
        public Double getFx() {
            return getDouble(FilePosition.FX);
        }

        /**
         * @return the so
         */
        public Double getSo() {
            return getDouble(FilePosition.SO);
        }

        /**
         * @return the nm
         */
        public Double getNm() {
            return getDouble(FilePosition.NM);
        }

        /**
         * @return the rr
         */
        public Double getRr() {
            return getDouble(FilePosition.RR);
        }

        /**
         * @return the pm
         */
        public Double getPm() {
            return getDouble(FilePosition.PM);
        }
        private Double getDouble(FilePosition fp){
            String s = result.get(fp).toString();
            if (s==null|| s.trim().isEmpty()){
                return null;
            }
            return Double.parseDouble(s);
        }
        @Override
        public String toString() {
            return (""+getStat()+";"+
                    outputformat.format(getDate())+";"+
                    getQn()+";"+
                    getTg()+";"+
                    getTn()+";"+
                    getTm()+";"+
                    getTx()+";"+
                    getRfm()+";"+
                    getFm()+";"+
                    getFx()+";"+
                    getSo()+";"+
                    getNm()+";"+
                    getRr()+";"+
                    getPm()).replaceAll("null", "");
        }
        
        
        
        
    }
    
    public static void main(String[] args) {
        for (DataBean b : parseStations()){
            System.out.println(b);
        }
    }
/**
 * getDataFromStation retrieves historic weather data from DWD 
 * @param station
 * @return a list of DataBeans 
 * @throws MalformedURLException
 * @throws ProtocolException
 * @throws IOException 
 */
    public static List<DataBean> getDataFromStation(String station) throws MalformedURLException, ProtocolException, IOException {
        String resultUrl = "https://www.dwd.de/DE/leistungen/klimadatendeutschland/klimadatendeutschland.html?view=renderJsonResults&undefined=Absenden&cl2Categories_LeistungsId=klimadatendeutschland&cl2Categories_Station="
                +station+"&cl2Categories_ZeitlicheAufloesung=klimadatendeutschland_tageswerte&cl2Categories_Format=text";
        List<DataBean> result = new ArrayList<>();
        URL url = new URL(resultUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String line=null;
            while((line=in.readLine())!=null){ //don't use streaming api, so it works in older java
                if (line.matches("\\d+.*")) {
                    DataBean d = new DataBean(line);
                    result.add(d);
                    //System.out.println(d);
                }
            }
        }
        return result;
    }
    /**
     * parseStations gets the list of weather stations and retrieves their data
     * All data is kept in memory and only returned when successful
     * @return a list of DataBeans
     */
    public static List<DataBean> parseStations()  {
        try {
            String stationsUrl = "https://www.dwd.de/DE/leistungen/klimadatendeutschland/klimadatendeutschland.json?view=renderJson&undefined=Absenden&cl2Categories_LeistungsId=klimadatendeutschland&cl2Categories_Station=klimadatendeutschland_berlintempelhof&cl2Categories_ZeitlicheAufloesung=klimadatendeutschland_tageswerte&cl2Categories_Format=text";
            final JsonObject json = loadJsonfromUrl(new URL(stationsUrl)).getAsJsonObject().get("cl2Categories_Station").getAsJsonObject();
            List<DataBean> result = new ArrayList<>();
            for (String s: json.keySet()){
                String station = json.get(s).getAsJsonObject().get("val").getAsString();
                result.addAll(getDataFromStation(station));
            }
            return result;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
           
        }
    }
    
    /**
     * Load json from a URL
     * @param url
     * @return a JsonElement 
     * @throws IOException 
     */
    private static JsonElement loadJsonfromUrl(URL url) throws IOException{
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        JsonElement e;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            JsonParser j = new JsonParser();
            e = j.parse(in);
        }
        return e;
    }
    
}
