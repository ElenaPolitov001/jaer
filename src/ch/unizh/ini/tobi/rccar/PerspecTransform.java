package ch.unizh.ini.tobi.rccar;

import ch.unizh.ini.caviar.chip.*;
import ch.unizh.ini.caviar.event.*;
import ch.unizh.ini.caviar.eventprocessing.EventFilter2D;
import ch.unizh.ini.caviar.graphics.FrameAnnotater;
import java.awt.Graphics2D;
import java.util.*;
import java.util.prefs.*;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.*;

/**
 * @author chbraen
 */
public class PerspecTransform extends EventFilter2D implements FrameAnnotater, Observer {
    public boolean isGeneratingFilter(){ return false;}
    private int horizon=getPrefs().getInt("PerspecTransform.horizon",90);
    {setPropertyTooltip("horizon","the height of the horizon (in pixles)");}
    private float horizonFactor=getPrefs().getFloat("PerspecTransform.horizonFactor",0);
    {setPropertyTooltip("horizonFactor","the curvature of the horizon");}
    private float ratio=getPrefs().getFloat("PerspecTransform.ratio",0.5f);
    {setPropertyTooltip("ratio","The ratio of the horizon to the with at the lower end of the picture");}
    private boolean lensEnabled=getPrefs().getBoolean("PerspecTransform.lensEnabled",false);
    {setPropertyTooltip("lenseEnabled","should the distortion of the lens be respected");}
    private float k1=getPrefs().getFloat("PerspecTransform.k1",0.001f);
    {setPropertyTooltip("k1","lense distortion coefficent 1");}
    
    private int[][] dx;
    private int[][] dy;
    private boolean[][] pass;
    private int sx;
    private float horizonFactorB;
    private int mx;
    private int sy;
    private int my;
    private float factor;
    private float alpha;
    private float r;
    private float ro;
    
    
    public PerspecTransform(AEChip chip){
        super(chip);
        resetFilter();
        chip.getCanvas().addAnnotator(this);
    }
    
    /**
     */
    synchronized public EventPacket filterPacket(EventPacket in) {
        if(in==null) return null;
        if(!filterEnabled) return in;
        if(dx == null) return in;
        if(enclosedFilter!=null) in=enclosedFilter.filterPacket(in);
        
        // filter
        
        int n=in.getSize();
        if(n==0) return in;
        checkOutputPacketEventType(in);
        OutputEventIterator outItr=out.outputIterator();
        resetFilter();
        
        // for each event only write it to the tmp buffers if it matches
        for(Object obj:in){
            TypedEvent e=(TypedEvent)obj;
            TypedEvent o=(TypedEvent)outItr.nextOutput();
            if(pass[e.x][e.y]){
                o.copyFrom(e);
                o.setX((short)(e.x+dx[e.x][e.y]));
                o.setY((short)(e.y+dy[e.x][e.y]));
            }
        }
        return out;
    }
    
    public Object getFilterState() {
        return null;
    }
    
    synchronized public void resetFilter() {
        if(chip == null) return;
        sx = chip.getSizeX();
        mx = sx/2;
        sy = chip.getSizeY();
        my = sy/2;
        dx = new int[sx][sy];
        dy = new int[sx][sy];
        pass = new boolean[sx][sy];
        buildMatrix();
    }
    
    public void initFilter() {
        resetFilter();
    }
    
    private void buildMatrix(){
        //the pass matrix has to be set up
        //the horizon is described by a hyperbole with y=horizonFactorB*x^2 + horizonFactor
        horizonFactorB=-horizonFactor/(mx*mx);
        for(int y=0; y<sy; y++){
            for(int x=0; x<sx; x++){
                if(y<horizon || ((y-horizon)<horizonFactorB*(x-mx)*(x-mx)+horizonFactor)) pass[x][y] = true;
                else pass[x][y] = false;
            }
        }
        //the transformation matrix has to be calculated
        for(int y=0; y<sy; y++){
            factor = (1-ratio)*((float)horizon - (float)y)/(float)horizon;
            for(int x=0; x<sx; x++){
                if(x!=mx){
                    alpha = (float)Math.atan(Math.abs(((float)my-(float)y)/((float)mx-(float)x)));
                } else {
                    alpha = (float)(Math.signum(x)*Math.PI/2);
                }
                ro = (float)Math.sqrt((mx-x)*(mx-x)+(my-y)*(my-y));
                if(lensEnabled){
                    r = lenseTransform(ro);
                    dx[x][y] = (int)((r/ro)*Math.cos(alpha)*Math.signum(mx-x));
                    dy[x][y] = (int)((r/ro)*Math.sin(alpha)*Math.signum(my-y));
                } else {
                    dx[x][y]=0;
                    dy[x][y]=0;
                }
                dx[x][y] = dx[x][y] + (short)((float)(mx-x)*factor);
                
            }
        }

        //-->uncomment to see the transformation matrix
        /*for(int y=horizon-1; y>=0; y--){
            for(int x=0; x<sx; x++){
                System.out.print(dx[x][y] + " ");
            }
            System.out.println();
        }*/
    }
    
    private float lenseTransform(float r){
        return (float)(r*(1+(k1*(r*r))));
    }
    
    public int getHorizon() {
        return horizon;
    }
    
    public void setHorizon(int horizon) {
        this.horizon = horizon;
        getPrefs().putInt("PerspecTransform.horizon",horizon);
        resetFilter();
    }
    
    public float getRatio() {
        return ratio;
    }
    
    public void setRatio(float ratio) {
        this.ratio = ratio;
        getPrefs().putFloat("PerspecTransform.ratio",ratio);
        resetFilter();
    }

    public float getHorizonFactor() {
        return horizonFactor;
    }
    
    public void setHorizonFactor(float horizonFactor) {
        this.horizonFactor = horizonFactor;
        getPrefs().putFloat("PerspecTransform.horizonFactor",horizonFactor);
        resetFilter();
    }
    
    public boolean isLensEnabled() {
        return lensEnabled;
    }
    
    public void setLensEnabled(boolean lensEnabled) {
        this.lensEnabled = lensEnabled;
        getPrefs().putBoolean("PerspecTransform.lensEnabled",lensEnabled);
        resetFilter();
    }
    
    public float getK1() {
        return k1;
    }
    
    public void setK1(float k1) {
        this.k1 = k1;
        getPrefs().putFloat("PerspecTransform.k1",k1);
        resetFilter();
    }
    
    public void annotate(float[][][] frame) {
    }
    
    public void annotate(Graphics2D g) {
    }
    
    public void annotate(GLAutoDrawable drawable) {
        if(!isAnnotationEnabled() || !isFilterEnabled()) return;
        if(!isFilterEnabled()) return;
        GL gl=drawable.getGL();
        gl.glPushMatrix();
        {
            
        }
        gl.glPopMatrix();
        
    }
    
    public void update(Observable o, Object arg) {
        chip.getCanvas().addAnnotator(this);
    }
    
}
