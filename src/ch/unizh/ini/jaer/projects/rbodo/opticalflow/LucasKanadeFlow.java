package ch.unizh.ini.jaer.projects.rbodo.opticalflow;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Level;
import net.sf.jaer.Description;
import net.sf.jaer.DevelopmentStatus;
import net.sf.jaer.chip.AEChip;
import net.sf.jaer.event.EventPacket;
import net.sf.jaer.event.PolarityEvent;
import net.sf.jaer.event.orientation.MotionOrientationEventInterface;

/**
 * Draws individual optical flow vectors and computes global motion, 
 * rotation and expansion. Algorithm is based on the method presented in
 * R. Benosmana, S.-H. Ieng, C. Clercq, C. Bartolozzi, M. Srinivasan, 
 * "Asynchronous frameless event-based optical flow", Neural Networks, 2012.
 * For each event, it uses a histogram of previous events in its neighborhood 
 * to estimate a spatial and temporal gradient. 
 * This serves as input data to an overdetermined system of linear equations 
 * that can be solved with Least Squares Estimation for the optical flow vector.
 * @author rbodo
 */

@Description("Class for amplitude and orientation of local motion optical flow using local gradient estimates.")
@DevelopmentStatus(DevelopmentStatus.Status.Experimental)
public class LucasKanadeFlow extends AbstractMotionFlow {
    // Store the timestamps for the last maxDtThreshold microseconds.
    // The size of the deque is proportional to the number of events 
    // that happened in the specified time-interval.
    private ArrayDeque<Integer>[][][] timestamps; 
    private ArrayDeque<Integer>[][][] timestamps2; 
    
    // Spatial and temporal derivatives in the neighborhood.
    private float[][] spatDerivNeighb;
    private float[] tempDerivNeighb;
    
    private float lambda1, lambda2;
    
    private float thr = getFloat("thr",1e6f);
    
    public enum DerivativeEstimator {
        BackwardFiniteDifference, 
        CentralFiniteDifferenceFirstOrder, 
        CentralFiniteDifferenceSecondOrder, 
        SavitzkyGolayFilter
    };

    private DerivativeEstimator derivator;

    private boolean backwardFiniteDifference, SavitzkyGolayFilter,
                    centralFiniteDifferenceFirstOrder,
                    centralFiniteDifferenceSecondOrder,
                    secondTempDerivative;
    
    // Current pixel location in middle of 1-d array containing all pixels of neighborhood.
    private int currPix;
    
    // Additional spacing to border of chip. Needed for finite difference methods.    
    private int d;
    
    private int[] neighb;
    private String deriv;
    
    public LucasKanadeFlow(AEChip chip) {
        super(chip);
        numInputTypes = 2;
        setDerivativeEstimator(DerivativeEstimator.CentralFiniteDifferenceFirstOrder);
        setPropertyTooltip("Lucas Kanade","thr","threshold to discard events with too small intensity gradient");
        setPropertyTooltip("Lucas Kanade","derivativeEstimator","select method to calculate intensity gradients");
        setPropertyTooltip("Lucas Kanade","secondTempDerivative","Use second temporal derivative");
    }

    @Override public void annotate(GLAutoDrawable drawable) {
        super.annotate(drawable);
        
        // Draws an event-histogram in the neighborhood of the event.
        // Used for debugging and visualization of the algorithm.
//        if (!isFilterEnabled()) return;
//        GL2 gl = drawable.getGL().getGL2();
//        if (gl == null) return;
//        checkBlend(gl);
//        ArrayDeque<Integer>[][][] timest = timestamps.clone();
//        for (i = 0; i < sizex; i++)
//            for (j = 0; j < sizey; j++) {
//                while (!timest[i][j][1].isEmpty() && dirPacket.getLastTimestamp() 
//                      > timest[i][j][1].peekFirst()+maxDtThreshold)
//                        timest[i][j][1].removeFirst();
//                gl.glPushMatrix();
//                gl.glColor4f(timest[i][j][1].size()/10f,timest[i][j][1].size()/10f, 0, 0.25f);
//                gl.glRectf(i,j,i+1,j+1);
//                gl.glPopMatrix();
//            }
//        
//        for(Object o : dirPacket){
//            gl.glPushMatrix();
//            MotionOrientationEventInterface ei = (MotionOrientationEventInterface) o;
//            for (sy = -searchDistance; sy <= searchDistance; sy++)
//                for (sx = -searchDistance; sx <= searchDistance; sx++) {
//                    gl.glColor4f(timestamps[ei.getX()+sx][ei.getY()+sy][ei.getType()].size()/100f,timestamps[ei.getX()+sx][ei.getY()+sy][ei.getType()].size()/100f, 0, 0.25f);
//                    gl.glRectf(ei.getX()+sx, ei.getY()+sy, ei.getX()+sx+1, ei.getY()+sy+1);
//                }
//            gl.glPopMatrix();
//        }
    }
    
    @Override final synchronized void allocateMap() {
        timestamps = new ArrayDeque[subSizeX][subSizeY][2];
        timestamps2 = new ArrayDeque[subSizeX][subSizeY][2];
        for (i = 0; i < timestamps.length; i++) {
            for (j = 0; j < timestamps[0].length; j++) {
                timestamps[i][j][0] = new ArrayDeque<>();
                timestamps[i][j][1] = new ArrayDeque<>();
                timestamps2[i][j][0] = new ArrayDeque<>();
                timestamps2[i][j][1] = new ArrayDeque<>();
            }
        }
        spatDerivNeighb = new float[(2*searchDistance+1)*(2*searchDistance+1)][2];
        tempDerivNeighb = new float[(2*searchDistance+1)*(2*searchDistance+1)];
        neighb = new int[(2*searchDistance+3)*(2*searchDistance+3)];
        currPix = 2*searchDistance*(searchDistance+1);
        super.allocateMap();
    }
    
    synchronized private void computeFittingParameters() {
        jj = 0;
        if (fitOrder == 1) {
            a[0][1] = 0;
            a[1][0] = 0;
            for (sy = -searchDistance; sy <= searchDistance; sy++)
                for (sx = -searchDistance; sx <= searchDistance; sx++) {
                    while (!timestamps[x+sx][y+sy][type].isEmpty() && 
                           ts > timestamps[x+sx][y+sy][type].peekFirst()+maxDtThreshold)
                        timestamps[x+sx][y+sy][type].removeFirst();
                    while (!timestamps2[x+sx][y+sy][type].isEmpty() && 
                           ts > timestamps2[x+sx][y+sy][type].peekFirst()+2*maxDtThreshold)
                        timestamps2[x+sx][y+sy][type].removeFirst();
                    a[0][1] += C[2][jj]*timestamps[x+sx][y+sy][type].size();
                    a[1][0] += C[1][jj]*timestamps[x+sx][y+sy][type].size();
                    jj++;
                }
        } else {
            for (sy = -searchDistance; sy <= searchDistance; sy++)
                for (sx = -searchDistance; sx <= searchDistance; sx++)
                    while (!timestamps[x+sx][y+sy][type].isEmpty() && 
                           ts > timestamps[x+sx][y+sy][type].peekFirst()+maxDtThreshold)
                        timestamps[x+sx][y+sy][type].removeFirst();
            ii = 0;
            for (j = 0; j <= fitOrder; j++)
                for (i = 0; i <= fitOrder-j; i++) {
                    a[i][j] = 0;
                    for (sy = -searchDistance; sy <= searchDistance; sy++)
                        for (sx = -searchDistance; sx <= searchDistance; sx++) 
                            a[i][j] += C[ii][jj++]*timestamps[x+sx][y+sy][type].size();
                    ii++;
                    jj = 0;
                }
        }
    }
        
    // <editor-fold defaultstate="collapsed" desc="Various derivation methods">
    /** 
     * Estimate spatial and temporal derivatives.
     * Loops consider only those events in the neighborhood that happened 
     * up to a time maxDtThreshold before current event.
     * Thus we have to update histograms of neighborhood first and discard the 
     * timestamps of events that are older than maxDtThreshold. Depending on the
     * derivation method, we have to go one or two steps beyond the neighborhood.
     */
    synchronized private void computeDerivatives() {
        if (!SavitzkyGolayFilter)
            for (sy = -searchDistance-d; sy <= searchDistance+d; sy++)
                for (sx = -searchDistance-d; sx <= searchDistance+d; sx++) {
                    while (!timestamps[x+sx][y+sy][type].isEmpty() && 
                           ts > timestamps[x+sx][y+sy][type].peekFirst()+maxDtThreshold)
                        timestamps[x+sx][y+sy][type].removeFirst();
                    while (!timestamps2[x+sx][y+sy][type].isEmpty() && 
                           ts > timestamps2[x+sx][y+sy][type].peekFirst()+2*maxDtThreshold)
                        timestamps2[x+sx][y+sy][type].removeFirst();     
                }
        ii = 0;
        for (sy = -searchDistance; sy <= searchDistance; sy++)
            for (sx = -searchDistance; sx <= searchDistance; sx++) {
                if (SavitzkyGolayFilter) {
                    if (fitOrder == 1) { // Direct computation saves time, and we seldom need general method.
                        spatDerivNeighb[ii][0] = (float) a[1][0];
                        spatDerivNeighb[ii][1] = (float) a[0][1];
                    } else {
                        spatDerivNeighb[ii][0] = 0;
                        spatDerivNeighb[ii][1] = 0;
                        for (j = 0; j <= fitOrder; j++)
                            for (i = 1; i <= fitOrder-j; i++)
                                spatDerivNeighb[ii][0] += (float) (i*a[i][j]*Math.pow(sx,i-1)*Math.pow(sy,j));
                        for (j = 1; j <= fitOrder; j++)
                            for (i = 0; i <= fitOrder-j; i++)
                                spatDerivNeighb[ii][1] += (float) (j*a[i][j]*Math.pow(sx,i)*Math.pow(sy,j-1));
                    }
                    tempDerivNeighb[ii] = timestamps[x+sx][y+sy][type].size();
                    if (secondTempDerivative)
                        tempDerivNeighb[ii] = timestamps[x+sx][y+sy][type].size()*2 -
                                             timestamps2[x+sx][y+sy][type].size();
                    tempDerivNeighb[ii] /= searchDistance;
                } else if (backwardFiniteDifference) {
                    spatDerivNeighb[ii][0] = timestamps[x+sx][y+sy][type].size() - 
                                             timestamps[x+sx-1][y+sy][type].size();
                    spatDerivNeighb[ii][1] = timestamps[x+sx][y+sy][type].size() - 
                                             timestamps[x+sx][y+sy-1][type].size();
                    tempDerivNeighb[ii]    = timestamps[x+sx][y+sy][type].size();
                    if (secondTempDerivative)
                        tempDerivNeighb[ii] = timestamps[x+sx][y+sy][type].size()*2 -
                                             timestamps2[x+sx][y+sy][type].size();
                } else if (centralFiniteDifferenceFirstOrder) {
                    spatDerivNeighb[ii][0] = timestamps[x+sx+1][y+sy][type].size() - 
                                             timestamps[x+sx-1][y+sy][type].size();
                    spatDerivNeighb[ii][1] = timestamps[x+sx][y+sy+1][type].size() - 
                                             timestamps[x+sx][y+sy-1][type].size();
                    tempDerivNeighb[ii] = timestamps[x+sx][y+sy][type].size()*2;
                    if (secondTempDerivative)
                        tempDerivNeighb[ii] = (timestamps[x+sx][y+sy][type].size()*2 -
                                              timestamps2[x+sx][y+sy][type].size())*2;
                } else if (centralFiniteDifferenceSecondOrder) {
                    spatDerivNeighb[ii][0] = timestamps[x+sx-2][y+sy][type].size()   - 
                                             timestamps[x+sx-1][y+sy][type].size()*8 +
                                             timestamps[x+sx+1][y+sy][type].size()*8 -
                                             timestamps[x+sx+2][y+sy][type].size();
                    spatDerivNeighb[ii][1] = timestamps[x+sx][y+sy-2][type].size()   - 
                                             timestamps[x+sx][y+sy-1][type].size()*8 +
                                             timestamps[x+sx][y+sy+1][type].size()*8 -
                                             timestamps[x+sx][y+sy+2][type].size();
                    tempDerivNeighb[ii]    = timestamps[x+sx][y+sy][type].size()*12;
                    if (secondTempDerivative)
                        tempDerivNeighb[ii] = (timestamps[x+sx][y+sy][type].size()*2 -
                                              timestamps2[x+sx][y+sy][type].size())*12;
                }
                // The temporal intensity gradient tempDerivNeighb is estimated 
                // by the event density events/s in the time interval maxDtThreshold.
                if (secondTempDerivative) tempDerivNeighb[ii] /= maxDtThreshold*1e-6f;
                tempDerivNeighb[ii] *= 20; // Derivative approximation contains some systematic error
                ii++;
            }
    }
        
    public DerivativeEstimator getDerivativeEstimator() {return derivator;}
    
    public final synchronized void setDerivativeEstimator(DerivativeEstimator derivator) {
        this.derivator = derivator;
        putString("derivator", derivator.toString());
        backwardFiniteDifference = false;
        centralFiniteDifferenceFirstOrder = false;
        centralFiniteDifferenceSecondOrder = false;
        SavitzkyGolayFilter = false;
        switch (derivator) {
            case BackwardFiniteDifference:
                backwardFiniteDifference = true;
                d = 1;
                resetFilter();
                break;
            case CentralFiniteDifferenceFirstOrder:
                centralFiniteDifferenceFirstOrder = true;
                d = 1;
                resetFilter();
                break;
            case CentralFiniteDifferenceSecondOrder:
                centralFiniteDifferenceSecondOrder = true;
                d = 2;
                resetFilter();
                break;
            case SavitzkyGolayFilter: 
                SavitzkyGolayFilter = true;
                d = 0;
                resetFilter();
                break;
            default: 
                centralFiniteDifferenceFirstOrder = true;
                d = 1;
                resetFilter();
                break;
        }
    }
    // </editor-fold>
    
    // Prints the event-rate function in the neighborhood of a certain pixel.
    // Used for debugging and visualization of algorithm in MATLAB.
    synchronized private void printNeighborhood() {
        deriv = "[";
        for (j = 0; j < spatDerivNeighb.length; j++)
            deriv += Arrays.toString(spatDerivNeighb[j]) + ";";
        deriv += "]";
        i = 0;
        for (sy = -searchDistance-1; sy <= searchDistance+1; sy++)
            for (sx = -searchDistance-1; sx <= searchDistance+1; sx++)
                neighb[i++] = timestamps[x+sx][y+sy][type].size();
        log.log(Level.INFO,String.format(Locale.ENGLISH,"z = %1$s; ds = %2$s; dt = %3$s; v = [%4$2.2f %5$2.2f]; vIMU = [%6$2.2f %7$2.2f];",
            new Object[]{Arrays.toString(neighb), deriv, Arrays.toString(tempDerivNeighb), vx, vy, vxGT, vyGT}));
    }

    @Override synchronized public EventPacket filterPacket(EventPacket in) {
        float p, q, tmp, tmp2, sx2, sy2, sxy, sxt, syt;
        setupFilter(in);
        
        for (Object ein : in) {
            extractEventInfo(ein);
            if (measureAccuracy || discardOutliersEnabled) {
                imuFlowEstimator.calculateImuFlow((PolarityEvent) inItr.next());
                setGroundTruth();
            }
            if (isInvalidAddress(searchDistance+d)) continue;
            timestamps[x][y][type].add(ts); // Add most recent event to queue.
            timestamps2[x][y][type].add(ts);
            if (!updateTimesmap()) continue;
            if (xyFilter()) continue;
            countIn++;
            if (SavitzkyGolayFilter) computeFittingParameters();
            computeDerivatives();
            // <editor-fold defaultstate="collapsed" desc="Solve for optical flow with LS">
            /**
             * With the least squares principle applied to data (A,b) the optical 
             * flow can be estimated by v = Inv(A'A)A'b. A RuntimeException may 
             * be thrown if the data matrix spatDerivNeighb is singular 
             * (linear dependent rows). A'A is invertible if its eigenvalues 
             * satisfy lambda1 >= lambda2 > 0. The eigenvalues are a measure for
             * the intensity gradient along the corresponding eigenvector. 
             * A high ratio r = lambda1/lambda2 results in noise and occurs for
             * events at edges, where the gradient along the edge vanishes.
             * A ratio r ~ 1 corresponds to approximately equal gradient along 
             * both eigenvectors.
             */
            sx2 = 0;
            sy2 = 0;
            sxy = 0;
            sxt = 0;
            syt = 0;
            for (i = 0; i < spatDerivNeighb.length; i++) {
                sx2 += spatDerivNeighb[i][0]*spatDerivNeighb[i][0];
                sy2 += spatDerivNeighb[i][1]*spatDerivNeighb[i][1];
                sxy += spatDerivNeighb[i][0]*spatDerivNeighb[i][1];
                sxt += spatDerivNeighb[i][0]*tempDerivNeighb[i];
                syt += spatDerivNeighb[i][1]*tempDerivNeighb[i];
                }
            p = sx2+sy2;
            q = sx2*sy2-sxy*sxy;
            tmp = (float) Math.sqrt(p*p-4*q);
            lambda1 = (p+tmp)/2;
            lambda2 = (p-tmp)/2;
            if (lambda1 < thr || Float.isNaN(lambda1)) {
                vx = 0;
                vy = 0;
            } else if (lambda2 < thr) {
                tmp2 = spatDerivNeighb[currPix][0]*spatDerivNeighb[currPix][0] + 
                       spatDerivNeighb[currPix][1]*spatDerivNeighb[currPix][1];
                if (tmp2 == 0) {
                    vx = 0;
                    vy = 0;
                } else {
                    vx = -spatDerivNeighb[currPix][0]*tempDerivNeighb[currPix]/tmp2;
                    vy = -spatDerivNeighb[currPix][1]*tempDerivNeighb[currPix]/tmp2;
                }
            } else {
                vx = (sxy*syt-sy2*sxt)/q;
                vy = (sxy*sxt-sx2*syt)/q;
            }
            v  = (float) Math.sqrt(vx*vx+vy*vy);
            // </editor-fold>
  
            if (accuracyTests()) continue;
            writeOutputEvent();
            if (measureAccuracy) motionFlowStatistics.update(vx,vy,v,vxGT,vyGT,vGT);
        }
        motionFlowStatistics.updatePacket(measureProcessingTime,showGlobalEnabled,
                                          countIn,countOut);
        return isShowRawInputEnabled() ? in : dirPacket;
    }
    // <editor-fold defaultstate="collapsed" desc="getter/setter for --thr--">
    public float getThr() {return this.thr;}

    public void setThr(final float thr) {
        this.thr = thr;
        putFloat("thr",thr);
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="getter/setter for --secondTempDerivative--">
    public boolean getSecondTempDerivative() {return this.secondTempDerivative;}

    public void setSecondTempDerivative(boolean secondTempDerivative) {
        this.secondTempDerivative = secondTempDerivative;
        putBoolean("secondTempDerivative",secondTempDerivative);
    }
    // </editor-fold>
}