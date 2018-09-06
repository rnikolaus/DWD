/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
    static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    static SimpleDateFormat outputformat = new SimpleDateFormat("dd.MM.yyyy");
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
        
        public static FilePosition getPosition(int pos){
            for (FilePosition s: FilePosition.values()){
                if (pos >=s.startPos && pos<s.endPos){
                    return s;
                }
            }
            throw new RuntimeException("position out of range: "+pos);
        } 
    }
    public static class  DataBean{
        
        private final Map<FilePosition,String> result;

        public DataBean(String line) {
            result= new HashMap<>();
            for (int pos =0;pos<line.length();pos++){
                        FilePosition fp = FilePosition.getPosition(pos);
                        if (!result.containsKey(fp)){
                            result.put(fp, "");
                        }
                        result.put(fp, result.get(fp).concat(""+line.charAt(pos)));
                    }
        }

        /**
         * @return the stat
         */
        public Long getStat() {
            return Long.parseLong(result.get(FilePosition.STAT));
        }

        /**
         * @return the date
         */
        public Date getDate(){
            try {
                return sdf.parse(result.get(FilePosition.DATE).trim());
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
            String s = result.get(fp);
            if (s==null|| s.trim().isEmpty()){
                return null;
            }
            return Double.parseDouble(s);
        }
        @Override
        public String toString() {
            return (""+getStat()+";"+outputformat.format(getDate())+";"+getQn()+";"+getTg()+";"+getTn()+";"+getTm()+";"+getTx()+";"+getRfm()+";"+getFm()+";"+getFx()+";"+getSo()+";"+getNm()+";"+getRr()+";"+getPm()).replaceAll("null", ""); //To change body of generated methods, choose Tools | Templates.
            
        }
        
        
        
        
    }
    
    public static void main(String[] args) {
        for (DataBean b : parseStations()){
            System.out.println(b);
        }
    }

    public static List<DataBean> getDataFromStation(String station) throws MalformedURLException, ProtocolException, IOException {
        String resultUrl = "https://www.dwd.de/DE/leistungen/klimadatendeutschland/klimadatendeutschland.html?view=renderJsonResults&undefined=Absenden&cl2Categories_LeistungsId=klimadatendeutschland&cl2Categories_Station="
                +station+"&cl2Categories_ZeitlicheAufloesung=klimadatendeutschland_tageswerte&cl2Categories_Format=text";
        List<DataBean> l = new ArrayList<>();
        URL url = new URL(resultUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String line=null;
            while((line=in.readLine())!=null){ //don't use streaming api, so it works in older java
                if (line.matches("\\d+.*")) {
                    DataBean d = new DataBean(line);
                    l.add(d);
                    //System.out.println(d);
                }
            }
        }
        return l;
    }

    public static List<DataBean> parseStations()  {
        try {
            String stationsUrl = "https://www.dwd.de/DE/leistungen/klimadatendeutschland/klimadatendeutschland.json?view=renderJson&undefined=Absenden&cl2Categories_LeistungsId=klimadatendeutschland&cl2Categories_Station=klimadatendeutschland_berlintempelhof&cl2Categories_ZeitlicheAufloesung=klimadatendeutschland_tageswerte&cl2Categories_Format=text";
            final JsonObject asJsonObject = loadJsonfromUrl(new URL(stationsUrl)).getAsJsonObject().get("cl2Categories_Station").getAsJsonObject();
            List<DataBean> l = new ArrayList<>();
            for (String s: asJsonObject.keySet()){
                String station = asJsonObject.get(s).getAsJsonObject().get("val").getAsString();
                l.addAll(getDataFromStation(station));
            }
            return l;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
           
        }
    }
    
    public static JsonElement loadJsonfromUrl(URL url) throws IOException{
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
