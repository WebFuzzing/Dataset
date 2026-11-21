package reciter.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author szd2013
 * 
 */
public class GenericJsonDeserializer {

	/**
	 * @return static Gson instance
	 */
	public static Gson getGson() {
        Gson gson = new GsonBuilder()
        		.setPrettyPrinting()
        		.create();
        return gson;
    }
	
	/**
	 * Generic cast o Object
	 * @param o
	 * @param clazz
	 * @return
	 */
	public static <T> T convertInstanceOfObject(Object o, Class<T> clazz) {
	    try {
	        return clazz.cast(o);
	    } catch(ClassCastException e) {
	        return null;
	    }
	}

}
