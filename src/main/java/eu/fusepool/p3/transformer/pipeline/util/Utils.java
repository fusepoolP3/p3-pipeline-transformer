package eu.fusepool.p3.transformer.pipeline.util;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Gabor
 */
public class Utils {

    /**
     * Get query parameters from a query string.
     *
     * @param queryString the query string
     * @return HashMap containing the query parameters
     */
    public static Map<String, String> getQueryParams(String queryString) throws ArrayIndexOutOfBoundsException {
        Map<String, String> temp = new HashMap<>();
        // query string should not be empty or blank
        if (StringUtils.isNotBlank(queryString)) {
            String[] params = queryString.split("&");
            String[] param;
            for (String item : params) {
                param = item.split("=", 2);
                temp.put(param[0], param[1]);
            }
        }
        return temp;
    }
}
