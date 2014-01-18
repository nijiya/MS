/*
 *  The MIT License
 * 
 *  Copyright 2010 Prasanna Velagapudi <psigen@gmail.com>.
 * 
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 * 
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 * 
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package tr.edu.metu.ceng.ms.thesis.robotutils.data;

import java.util.List;
import java.util.Map;

import tr.edu.metu.ceng.ms.thesis.modstarlite.data.ObjectiveArray;

/**
 *
 * @author Prasanna Velagapudi <psigen@gmail.com>
 */

/**
 * A simple tuple class that represents a real-valued coordinate
 * in arbitrary dimensions.  Equality, hashcode and comparisons should all be
 * implemented as a lexical ordering over the tuple elements.  Comparison and
 * equality between different implementations should imply that differences are
 * within the numerical error constant.
 */
public interface Coordinate {
    
    public double[] get();
    public double get(int dim);
    public int dims();
    public int getInsertionCount();
    public void inserted();
    
    //used in path construction.
    public Map<Coordinate, List<ObjectiveArray>> getParents();
    public boolean isExpanded();
    public void setExpanded(boolean expanded);
    public void incrExpansion();
    public int getExpansion();
    
//    public double getRisk();
//    public void setRisk(double risk);
    
//    public List<List<ObjectiveArray>> getCostToReach();
//    public void setCostToReach(List<List<ObjectiveArray>> costList);
}
