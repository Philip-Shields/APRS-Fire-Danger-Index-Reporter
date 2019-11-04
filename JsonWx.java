import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDate;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;




public class JsonWx {
	static float fireRating; //FDI     	
	static double windSpeed;
	static double humidity;
	static double airTemp;
	static String rain;
	static double consecutiveDryDays;
	static String windDirection;
	static long forwardTravel;
	static long fuelMoisture;
	static Date weatherNowDate;
	static double qnh;
	static double gust;
	
	static String callSign="VK2CPR-13";
	static int passCode=18360;		//aprs password	
	static String icon; //the aprs icon number corresponding to severity 0-5
	static String humanReadable; //low medium high etc
		
	static String townURL="http://www.bom.gov.au/fwo/IDN60801/IDN60801.95896.json"; //Albury nsw 
	
	//String serverName="second.aprs.net";
	//int port = 20157;
	//String serverName="rotate.aprs.net.au"; 
	static String serverName="aunz.aprs2.net";
	static int port=14580;
	static int i;//aprs telemetry number
	static String last; //last weather date
	//int port=14501;
	
	//external documents
	
	static String fileTelem = new File("C:\\java\\JsonWx\\telemNumber.txt").getAbsolutePath();//incremental aprs telemetry beacon
	static String fileLastRain = new File("C:\\java\\JsonWx\\lastRainAlbury.txt").getAbsolutePath();//last rain
	static String weatherNow = new File("Z:\\wxnow.txt").getAbsolutePath(); //cumulus weather file
	
	public static void main(String[] args) throws IOException {
		
		Timer thirtyMin = new Timer(); // main timer
		ThirtyMinScheduledTask thirtyMinTimer = new ThirtyMinScheduledTask(); // Instantiate SheduledTask class
		thirtyMin.schedule(thirtyMinTimer, 0, 1800*1000); // run the program every thirty mins
		          	
    }
	
	public static void JsonFromBom() throws IOException{
		// Gson gson = new GsonBuilder().enableComplexMapKeySerialization()
        //.setPrettyPrinting().serializeNulls().create();



//constructors
	Gson gson = new GsonBuilder().serializeNulls().create();
	TownWx townWx = new TownWx();        
    Observations observations = new Observations();        
    NoticeDataset noticeDataset = new NoticeDataset();
    HeaderDataset headerDataset = new HeaderDataset();
    WeatherData weatherData = new WeatherData();      
    
    
//Get the classes and make datasets to match the original BOM json structure
    townWx.setObservations(observations);        
    observations.setNotice(new NoticeDataset[] { noticeDataset });
    observations.setHeader(new HeaderDataset[] { headerDataset });
    observations.setWeatherData(new WeatherData[] {weatherData});
    
//get the Albury weather json from BOM
    URL url = new URL(townURL);
    BufferedReader bufferedReader = new BufferedReader(
        new InputStreamReader(url.openStream()));		    
    TownWx wxObject = gson.fromJson(bufferedReader, TownWx.class);
    	bufferedReader.close();		   

    	//weather variables from json object right now
    	airTemp = wxObject.observations.data[0].getAir_temp();
    	 windSpeed = wxObject.observations.data[0].getWind_spd_kmh();
    	humidity = wxObject.observations.data[0].getRel_hum();
    	windDirection = wxObject.observations.data[0].getWind_dir();
    	rain = wxObject.observations.data[0].getRain_trace();
    	qnh = wxObject.observations.data[0].getPress_qnh();
    	gust = wxObject.observations.data[0].getGust_kmh();
    	
    	if(rain.equals("-")) {
    		rain="0.0";
    	}
    	
    	ArrayList<String> lastRainDate = new ArrayList<String>();
    	System.out.println("Air temp= "+airTemp+"c");
    	System.out.println("Wind speed= "+windSpeed+" Km/hr "+ "Direction= "+windDirection);
    	System.out.println("Humidity= "+humidity+"%");
    	System.out.println("Rain= "+rain+" mm");
    	System.out.println("date now= "+wxObject.observations.data[0].getLocal_date_time());
    	
    	//scan rain trace for an entry that isn't 0.0 
    	for(int i=0; i<=wxObject.observations.data.length-1;i++) {
    		if(wxObject.observations.data[i].getRain_trace().equals("0.0")||wxObject.observations.data[i].getRain_trace().equals("-")) {
    			
        	}else {lastRainDate.add(wxObject.observations.data[i].getLocal_date_time_full());
    			
    		}
    		
    	}
    	if(!lastRainDate.isEmpty())
    	 {
    		WriteLastRainToFile(lastRainDate.get(0));   
    		System.out.println("last rain date array"+ lastRainDate);
		}
    	
    	
    	//DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    	 
    	 LocalDate dateNow = LocalDate.now();
    	
    	 LocalDate lastRain = LocalDate.parse(LastRainDateReader(), formatter); 
    	 
    	System.out.println("Time now= "+ dateNow); //2016/11/16 12:08:43
    	System.out.println("Last rain= "+lastRain);
    	
    	System.out.println("Consecutive dry days= "+ChronoUnit.DAYS.between(lastRain,dateNow));
    	consecutiveDryDays= ChronoUnit.DAYS.between(lastRain,dateNow);
    	double fireRating=Algorithms(windSpeed, humidity, airTemp, rain, consecutiveDryDays);
    	
    	System.out.println("Fire rating= "+fireRating);
	}
	
	
		
		//getter setters from the BOM json stream
	//this makes more sense if you look at the json at townURL
	public static class TownWx {
	 
    
    private Observations observations;

	public Observations getObservations() {
		return observations;
	}

	public void setObservations(Observations observations) {
		this.observations = observations;
	}
	
	
}

	public static class Observations{
	private NoticeDataset[] notice;
	private HeaderDataset[] header; 
	private WeatherData[] data;
	
	public NoticeDataset[] getNotice() {
		return notice;
	}
	public void setNotice(NoticeDataset[] notice) {
		this.notice = notice;
	}
	public HeaderDataset[] getHeader() {
		return header;
	}
	public void setHeader(HeaderDataset[] header) {
		this.header = header;
	}
	public WeatherData[] getWeatherData() {
		return data;
	}
	public void setWeatherData(WeatherData[] weatherData) {
		this.data = weatherData;
	}
	}

	public static class NoticeDataset {
    private String copyright;
    private String copyright_url;
    private String disclaimer_url;
    private String feedback_url;
    
	public String getCopyright() {
		return copyright;
	}
	public void setCopyright(String copyright) {
		this.copyright = copyright;
	}
	public String getCopyright_url() {
		return copyright_url;
	}
	public void setCopyright_url(String copyright_url) {
		this.copyright_url = copyright_url;
	}
	public String getDisclaimer_url() {
		return disclaimer_url;
	}
	public void setDisclaimer_url(String disclaimer_url) {
		this.disclaimer_url = disclaimer_url;
	}
	public String getFeedback_url() {
		return feedback_url;
	}
	public void setFeedback_url(String feedback_url) {
		this.feedback_url = feedback_url;
	}
    
 
}

	public static class HeaderDataset{
	 	private String refresh_message;
	    private String ID;
	    private String main_ID;
	    private String name;
	    private String state_time_zone;
	    private String time_zone;
	    private String product_name;
	    private String state;
	    
		public String getRefresh_message() {
			return refresh_message;
		}
		public void setRefresh_message(String refresh_message) {
			this.refresh_message = refresh_message;
		}
		public String getID() {
			return ID;
		}
		public void setID(String iD) {
			ID = iD;
		}
		public String getMain_ID() {
			return main_ID;
		}
		public void setMain_ID(String main_ID) {
			this.main_ID = main_ID;
		}
		public String getState_time_zone() {
			return state_time_zone;
		}
		public void setState_time_zone(String state_time_zone) {
			this.state_time_zone = state_time_zone;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getTime_zone() {
			return time_zone;
		}
		public void setTime_zone(String time_zone) {
			this.time_zone = time_zone;
		}
		public String getProduct_name() {
			return product_name;
		}
		public void setProduct_name(String product_name) {
			this.product_name = product_name;
		}
		public String getState() {
			return state;
		}
		public void setState(String state) {
			this.state = state;
		}
}

	//the actual observations 
	public static class WeatherData{
	private int sort_order;
	private int wmo;
	private String name;
	private String history_product;
	private String local_date_time;
	private String local_date_time_full;
	private String aifstime_utc;
	private double lat;
	private double lon;
	private double apparent_t;
	private String cloud;
	private String cloud_base_m;
	private String cloud_oktas;
	private String cloud_type;
	private String cloud_type_id;
	private double delta_t;
	private double gust_kmh;
	private double gust_kt;
	private double air_temp;
	private double dewpt;
	private double press;
	private double press_msl;
	private double press_qnh;
	private String press_tend;
	private String rain_trace;
	private double rel_hum;
	private String sea_state;
	private String swell_dir_worded;
	private String swell_height;
	private String swell_period;
	private String vis_km;
	private String weather;
	private String wind_dir;
	private double wind_spd_kmh;
	private double wind_spd_kt;
	
	public int getSort_order() {
		return sort_order;
	}
	public void setSort_order(int sort_order) {
		this.sort_order = sort_order;
	}
	public int getWmo() {
		return wmo;
	}
	public void setWmo(int wmo) {
		this.wmo = wmo;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getHistory_product() {
		return history_product;
	}
	public void setHistory_product(String history_product) {
		this.history_product = history_product;
	}
	public String getLocal_date_time() {
		return local_date_time;
	}
	public void setLocal_date_time(String local_date_time) {
		this.local_date_time = local_date_time;
	}
	public String getLocal_date_time_full() {
		return local_date_time_full;
	}
	public void setLocal_date_time_full(String local_date_time_full) {
		this.local_date_time_full = local_date_time_full;
	}
	public String getAifstime_utc() {
		return aifstime_utc;
	}
	public void setAifstime_utc(String aifstime_utc) {
		this.aifstime_utc = aifstime_utc;
	}
	public double getLat() {
		return lat;
	}
	public void setLat(double lat) {
		this.lat = lat;
	}
	public double getLon() {
		return lon;
	}
	public void setLon(double lon) {
		this.lon = lon;
	}
	public double getApparent_t() {
		return apparent_t;
	}
	public void setApparent_t(double apparent_t) {
		this.apparent_t = apparent_t;
	}
	public String getCloud() {
		return cloud;
	}
	public void setCloud(String cloud) {
		this.cloud = cloud;
	}
	public String getCloud_base_m() {
		return cloud_base_m;
	}
	public void setCloud_base_m(String cloud_base_m) {
		this.cloud_base_m = cloud_base_m;
	}
	public String getCloud_oktas() {
		return cloud_oktas;
	}
	public void setCloud_oktas(String cloud_oktas) {
		this.cloud_oktas = cloud_oktas;
	}
	public String getCloud_type() {
		return cloud_type;
	}
	public void setCloud_type(String cloud_type) {
		this.cloud_type = cloud_type;
	}
	public String getCloud_type_id() {
		return cloud_type_id;
	}
	public void setCloud_type_id(String cloud_type_id) {
		this.cloud_type_id = cloud_type_id;
	}
	public double getDelta_t() {
		return delta_t;
	}
	public void setDelta_t(double delta_t) {
		this.delta_t = delta_t;
	}
	public double getGust_kmh() {
		return gust_kmh;
	}
	public void setGust_kmh(double gust_kmh) {
		this.gust_kmh = gust_kmh;
	}
	public double getGust_kt() {
		return gust_kt;
	}
	public void setGust_kt(double gust_kt) {
		this.gust_kt = gust_kt;
	}
	public double getAir_temp() {
		return air_temp;
	}
	public void setAir_temp(double air_temp) {
		this.air_temp = air_temp;
	}
	public double getDewpt() {
		return dewpt;
	}
	public void setDewpt(double dewpt) {
		this.dewpt = dewpt;
	}
	public double getPress() {
		return press;
	}
	public void setPress(double press) {
		this.press = press;
	}
	public double getPress_msl() {
		return press_msl;
	}
	public void setPress_msl(double press_msl) {
		this.press_msl = press_msl;
	}
	public double getPress_qnh() {
		return press_qnh;
	}
	public void setPress_qnh(double press_qnh) {
		this.press_qnh = press_qnh;
	}
	public String getPress_tend() {
		return press_tend;
	}
	public void setPress_tend(String press_tend) {
		this.press_tend = press_tend;
	}
	public String getRain_trace() {
		return rain_trace;
	}
	public void setRain_trace(String rain_trace) {
		this.rain_trace = rain_trace;
	}
	public double getRel_hum() {
		return rel_hum;
	}
	public void setRel_hum(double rel_hum) {
		this.rel_hum = rel_hum;
	}
	public String getSea_state() {
		return sea_state;
	}
	public void setSea_state(String sea_state) {
		this.sea_state = sea_state;
	}
	public String getSwell_dir_worded() {
		return swell_dir_worded;
	}
	public void setSwell_dir_worded(String swell_dir_worded) {
		this.swell_dir_worded = swell_dir_worded;
	}
	public String getSwell_height() {
		return swell_height;
	}
	public void setSwell_height(String swell_height) {
		this.swell_height = swell_height;
	}
	public String getSwell_period() {
		return swell_period;
	}
	public void setSwell_period(String swell_period) {
		this.swell_period = swell_period;
	}
	public String getVis_km() {
		return vis_km;
	}
	public void setVis_km(String vis_km) {
		this.vis_km = vis_km;
	}
	public String getWeather() {
		return weather;
	}
	public void setWeather(String weather) {
		this.weather = weather;
	}
	public String getWind_dir() {
		return wind_dir;
	}
	public void setWind_dir(String wind_dir) {
		this.wind_dir = wind_dir;
	}
	public double getWind_spd_kmh() {
		return wind_spd_kmh;
	}
	public void setWind_spd_kmh(double wind_spd_kmh) {
		this.wind_spd_kmh = wind_spd_kmh;
	}
	public double getWind_spd_kt() {
		return wind_spd_kt;
	}
	public void setWind_spd_kt(double wind_spd_kt) {
		this.wind_spd_kt = wind_spd_kt;
	}
	
	
	}
	
	//algorithms that calculate the fire danger
	public static double Algorithms (double windSpeed, double humidity, double airTemp, String rain, double consecutiveDryDays){
		
		int grassCuring=80; //grass curing 1 to 100%
		double rainValue = Double.parseDouble(rain);
		/**Keetch Byram Drought Index (BKDI) provides an estimate of soil dryness (moisture deficiency). 
		 * The number indicates the amount of rainfall in mm that would be required to reduce the index to zero or saturation. 
		 * The meanings of the various KBDI ranges are as follows:
		\95 0 \96 24mm Mild
		\95 25 \96 62mm Average
		\95 63 \96 100mm Serious
		\95 101 \96 200mm Extreme */
		
		double BKDI = 60;
		
		//Calculate Drought Factor
		double droughtFactor = Math.round(Math.pow(0.191*(BKDI+104)*(consecutiveDryDays+1),1.5)/(Math.pow(3.52*(consecutiveDryDays+1),1.5)+rainValue-1)); 
		if (droughtFactor>10)droughtFactor = 10;
		
		//Fuel moisture content
		fuelMoisture=Math.round(((97.7+(4.06*humidity))/(airTemp+6))-(0.00854*humidity)+(3000/grassCuring)-30); 
		
		//Fire rating
		//fireRating= ((3 * Math.round((Math.exp ((.987 *Math.log(DF + 0.001)) - .45 - (.0345 * Humid)+ (.0338 *Temp)+ (.0234 * WindNow))))));
		fireRating = 2 * (Math.round(Math.exp(-.45 + .987 * Math.log(droughtFactor + .001) - .0345 * humidity + .0338 * airTemp + .0234 * windSpeed)));   
		
		//rate of forward spread of fire on level grassland
		forwardTravel=Math.round(((0.13*fireRating)*100)/100); 
		System.out.println("Drought factor= "+droughtFactor);
		//System.out.println("Fire danger= "+fireRating);
		icon=BeaconIcon(fireRating);
    	humanReadable=FireDangerReadable(fireRating);
    	//AprsBeacons();
		return fireRating;
    } 
	
	//human readable fire danger scale
	public static String FireDangerReadable(double fireRating) {
		String rating="";
		
		if (fireRating<11) {
			rating="LOW";
		
		}

			if (fireRating>=11) {
		rating = "HIGH";
		
		}

			if (fireRating>=24) {
		rating = "VERY HIGH";
		
		}

			if (fireRating>=49) {
		rating = "SEVERE";
		
		}

			if (fireRating>=74) {
		rating = "EXTREME";
		
		}

			if (fireRating>=99) {
		rating = "CODE RED";
		
		}


			System.out.println("Fire rating " + rating);
			
			return rating;
			
	}
	
	//aprs icon increments as per the severity 
	public static String BeaconIcon(double fireRating) {
		String icon="0";
		if (fireRating<11) {
			
		icon="0";
		}

			if (fireRating>=11) {
		
		icon="1";
		}

			if (fireRating>=24) {
		
		icon="2";
		}

			if (fireRating>=49) {
		
		icon="3";
		}

			if (fireRating>=74) {
		
		icon="4";
		}

			if (fireRating>=99) {
		
		icon="5";
		}
			return icon;
	}
	
	public static void AprsBeacons(){     
		try
		      {

			String logOn="user "+ callSign+" pass "+passCode+" ver \"manual login\"";
			//object beacon
			String object="FireDangr>APRS,TCPIP*:!3614.16S/14642.03E"+icon+Math.round(consecutiveDryDays)+" days since it last rained. FDI="+Math.round(fireRating)+"/100; which is "+humanReadable;
		
			String telem="FireDangr>APRS,TCPIP*:T#"+TelemetryBeaconNumberReader()+","+Math.round(fireRating)+","+windSpeed+","+airTemp+","+fuelMoisture+","+forwardTravel+",";
		//send the beacons down the pipe

		System.out.println("Connecting to: " + serverName + " on port: " + port);
		 Socket client = new Socket(serverName, port);
		  System.out.println("Just connected to: "+ client.getRemoteSocketAddress());
		    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
			   
		        out.newLine();
			out.flush();
		 System.out.println("Server says: " + in.readLine());
		      out.write(logOn);
			out.newLine();
			out.flush();
		System.out.println("I says: " + logOn);
			   System.out.println("Server says: " + in.readLine());
		      

		out.write(object);
		out.newLine();
		out.flush();
		System.out.println("I says: " + object);

		System.out.println("Server says: " + in.readLine());


		out.write(telem);
		out.newLine();
		out.flush();
		System.out.println("I says: " + telem);


		System.out.println("Server says: " + in.readLine());



		client.close();
		      }catch(IOException e)
		      {
		         e.printStackTrace();
		      }
		TelemetryBeaconNumberWriter();
		      }
	
	
	//aprs needs a special beacon number
	
	public static int TelemetryBeaconNumberReader(){
		   
		try{ 
		FileReader fileReader = new FileReader(fileTelem);
		 BufferedReader bufferedReader = new BufferedReader(fileReader);

		    i = Integer.parseInt(bufferedReader.readLine());
		//System.out.println("Reader number= "+i);

		bufferedReader.close();

		    } catch (IOException e) {e.printStackTrace();} 
		return i;
		
		}
	
	
	public static void TelemetryBeaconNumberWriter(){

		//telemetry frame sequential number writer 
		try {

		      FileWriter fileWriter =new FileWriter(fileTelem);           
		BufferedWriter bufferedWriter =new BufferedWriter(fileWriter);
		      
		 //telemetry frame numbers reset to 1 
		  i=i+1;
		if (i>=255)
		i=1;

		//System.out.println("Writer number= "+i);



		            bufferedWriter.write(new Integer(i).toString());
		            
		            bufferedWriter.newLine();		            

		           bufferedWriter.close();


		        }
		        catch(IOException ex) {
		            System.out.println(
		                "Error writing to file '"
		                + fileTelem + "'");
		            
		        }
		}
		
	
	public static String LastRainDateReader() throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(fileLastRain));
	   String last = br .readLine();
	   System.out.println("First line is : " + last);
	   br.close();
		return last;		
	}
	
	public static void WriteLastRainToFile(String date) throws IOException {
		 FileWriter fileWriter =new FileWriter(fileLastRain);           
			BufferedWriter bufferedWriter =new BufferedWriter(fileWriter);
			 bufferedWriter.write(date);
			 bufferedWriter.newLine();
			 bufferedWriter.close();
	}
	
	//Write a wxnow.txt file in the UI-View32 format	
	public static void WriteWeatherNow(double windSpeed, double humidity,double airTemp,
	String rain, String windDirection, double qnh, double gust) throws IOException {
		Date dateNow = new Date( ); 
		SimpleDateFormat ft = new SimpleDateFormat ("MMM dd yyyy H:mm");
		String date=ft.format(dateNow);
		
		//change to compass bearings
        String bearing;
        switch (windDirection) {
            case "N":  bearing = "000";
                     break;
            case "NNE":  bearing = "023";
                     break;
            case "NE":  bearing = "045";
                     break;
            case "ENE":  bearing = "068";
                     break;
            case "E":  bearing = "090";
                     break;
            case "ESE":  bearing = "113";
                     break;
            case "SE":  bearing = "135";
                     break;
            case "SSE":  bearing = "158";
                     break;
            case "S":  bearing = "180";
                     break;
            case "SSW":  bearing = "203";
                     break;
            case "SW":  bearing = "225";
                     break;
            case "WSW":  bearing = "248";
                     break;
            case "W":  bearing = "270";
            break;
            case "WNW":  bearing = "293";
            break;
            case "NW":  bearing = "315";
            break;
            case "NNW":  bearing = "338";
            break;
            
            default: bearing = "000";
            break; 
            
        }
        
        // change from metric to imperial
        windSpeed=windSpeed*0.6213711922; //km/h to mph
        gust=gust*0.6213711922; //km/h to mph
        airTemp=(airTemp*1.8)+32;//celcius to faranheight
        
      //round to get rid of decimal point
        int s=(int)windSpeed;
        int h=(int)humidity;
        int t=(int)airTemp; 
        int b=(int)qnh;
        int g=(int)gust;
        
       double ra=Double.parseDouble(rain); //rain is a string so change to double
       ra=(ra/25.4)*100; //100ths of an inch rain
       int rn = (int)ra; 
       
       System.out.println("ra= "+ra);      
       System.out.println("rn= "+rn);
       
       
        //Turn into string to write with the correct number of digits for APRS
        String sp = String.format("%02d", s);
        String bp = String.format("%04d", b);
        String humid = String.format("%02d", h);
        String tmp = String.format("%02d", t);
        String rainSinceMidnight = String.format("%03d", rn);
        String gu = String.format("%03d", g);
		
		 FileWriter fileWriter =new FileWriter(weatherNow);           
			BufferedWriter bufferedWriter =new BufferedWriter(fileWriter);
			 bufferedWriter.write(date);
			 bufferedWriter.newLine();
			 bufferedWriter.write(bearing+"/0"+sp+"g"+gu+"t0"+tmp+"P"+rainSinceMidnight+"h"+humid+"b"+bp+"0");
			 bufferedWriter.close();
			 System.out.println(date);
			 System.out.println(bearing+"/0"+sp+"g"+gu+"t0"+tmp+"P"+rainSinceMidnight+"h"+humid+"b"+bp+"0");
	}
	
	//do the work
	public static class ThirtyMinScheduledTask extends TimerTask {
			
			public void run() {
				
				try {
					JsonWx.JsonFromBom();
					JsonWx.AprsBeacons();
					JsonWx.WriteWeatherNow(windSpeed, humidity, airTemp, rain, windDirection,qnh,gust);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}		        	
	        	
			}
			
		}
}

