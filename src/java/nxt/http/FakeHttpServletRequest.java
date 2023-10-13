/******************************************************************************
 * Copyright © 2014-2016 Krypto Fin ry and FIMK Developers.                   *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * FIMK software, including this file, may be copied, modified, propagated,   *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package nxt.http;

import java.util.ArrayList;
import java.util.List;

public class FakeHttpServletRequest extends FakeHttpServletRequestBase {
  
    static class Param {
        String name;
        String value;
        
        public Param(String name, String value) {
            this.name = name;
            this.value = value;
        }      
    }
    
    List<Param> parameters = new ArrayList<Param>();
    
    public void addParameter(String name, String value) {
        parameters.add(new Param(name, value));
    }
  
    @Override
    public String getParameter(String name) {
        for (Param param : parameters) {
            if (param.name.equalsIgnoreCase(name)) {
                return param.value;
            }
        }
        return null;
    }
    
    @Override
    public String[] getParameterValues(String name) {
        List<String> result = new ArrayList<String>();
        for (Param param : parameters) {
            if (param.name.equalsIgnoreCase(name)) {
                result.add(param.value);
            }
        }
        String[] array = new String[result.size()];
        return result.toArray(array);
    }
  
}
