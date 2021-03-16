//    Chromis POS  - The New Face of Open Source POS
/*
**    Chromis POS  - The New Face of Open Source POS
**    Copyright (c)2015-2016
**    http://www.chromis.co.uk
**
**    This file is part of Chromis POS Version V0.60.2 beta
**
**    Chromis POS is free software: you can redistribute it and/or modify
**    it under the terms of the GNU General Public License as published by
**    the Free Software Foundation, either version 3 of the License, or
**    (at your option) any later version.
**
**    Chromis POS is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
**    GNU General Public License for more details.
**
**    You should have received a copy of the GNU General Public License
**    along with Chromis POS.  If not, see <http://www.gnu.org/licenses/>
**
**
*/

package uk.chromis.pos.scripting;

import bsh.EvalError;
import bsh.Interpreter;

/**
 *
 * @author adrianromero
 * Created on 5 de marzo de 2007, 19:57
 *
 */
class ScriptEngineBeanshell implements ScriptEngine {

    private Interpreter i;
    
    /** Creates a new instance of ScriptEngineBeanshell */
    public ScriptEngineBeanshell() {
        i = new Interpreter();
    }
    
    @Override
    public void put(String key, Object value) {
        
        try {
            i.set(key, value);
        } catch (EvalError e) {
        }
    }
    
    @Override
    public Object get(String key) {
        
        try {
            return i.get(key);
        } catch (EvalError e) {
            return null;
        }
    }
    
    @Override
    public Object eval(String src) throws ScriptException {

        try {
            return i.eval(src);  
        } catch (EvalError e) {
            throw new ScriptException(e.getMessage(), e);
        }        
    }   
}
