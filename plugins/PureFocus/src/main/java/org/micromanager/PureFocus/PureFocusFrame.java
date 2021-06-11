/**
@file PureFocusFrame.java
@author Graham Bartlett
@copyright Prior Scientific Instruments Limited, 2021
@brief Micro-Manager plugin for control of the Prior PureFocus PF-850 autofocus unit

Micro-Manager plugin giving GUI control of the PF-850 setup and configuration.

Licensed under the BSD license.
*/

package org.micromanager.PureFocus;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerModel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import mmcorej.CMMCore;
import mmcorej.StrVector;
import mmcorej.DeviceType;
import net.miginfocom.swing.MigLayout;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import org.micromanager.Studio;
import org.micromanager.internal.utils.WindowPositioning;


/** Top-level GUI for the PureFocus plugin.
 * 
 * This is only instantiated by the plugin if a PureFocus PF-850 device exists.
 * The GUI can therefore assume that calls to the device adapter will work,
 * barring the normal errors of invalid values or loss of comms.
 * 
 * @todo Currently this is just a placeholder to get us started.
 * 
 * @todo We need a menu or buttons to launch child dialogs.
 */
@SuppressWarnings(value = {"serial", "static-access"})    
public class PureFocusFrame extends JFrame implements ActionListener, ChangeListener
{
	private final Studio gui_;
	private final CMMCore core_;
	private final PureFocus plugin_;
    
    private Timer timer_;
    private Boolean errorShown_;
    
	// GUI elements
	private JSpinner objectiveSelectSpinner_;
    private JTextField offsetPositionMicrons_;
    private JTextField focusPositionMicrons_; 
    private JTextField calculationA_;
    private JTextField calculationB_;
    private JTextField calculationC_;
    private JTextField calculationD_;
    private JTextField focusPidTarget_;
    private JTextField focusPidPosition_;
    private JTextField focusPidError_;
    private JTextField focusPidOutput_;
    private JTextField focusState_;
    private JTextField timeToInFocus_;
    private JCheckBox isOffsetMoving_;
    private JCheckBox isFocusDriveMoving_;
    private JCheckBox positiveLimitSwitch_;    
    private JCheckBox negativeLimitSwitch_;    
    
    
    // Child dialogs
    PureFocusObjectiveSlotTableDialog objectiveSlotTableDialog_;
    PureFocusGlobalTableDialog globalTableDialog_;

    
   
	/** Creates new form PureFocusFrame
	@param gui MM scriptInterface
	@param plugin Holds on to parent plugin
	*/
	public PureFocusFrame(Studio gui, PureFocus plugin) 
	{
		gui_ = gui;
		core_ = gui.getCMMCore();
		plugin_ = plugin;

        errorShown_ = false;
        
		// If we get here, we know we have a PureFocus connected
        initComponents();

        super.setIconImage(Toolkit.getDefaultToolkit().getImage(
                    getClass().getResource("/org/micromanager/icons/microscope.gif")));

        updateValues(true);
        
        // Run timed updates of form
        timer_ = new Timer(2000, this);
        timer_.setInitialDelay(2000);
        timer_.start(); 

        objectiveSlotTableDialog_ = new PureFocusObjectiveSlotTableDialog(this, plugin_, gui_);
        objectiveSlotTableDialog_.setVisible(true);
        
        globalTableDialog_ = new PureFocusGlobalTableDialog(this, plugin_, gui_);
        globalTableDialog_.setVisible(true);        
	}


    private void updateValues(boolean allValues)
	{
        Boolean errored = false;
        String errorMessage = "";
        
		try
		{
			String val;
 
            if (allValues)
            {
                val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.OBJECTIVE);
                SpinnerModel numberModel = objectiveSelectSpinner_.getModel();
                numberModel.setValue(Integer.parseInt(val));
                
                offsetPositionMicrons_.setText(core_.getProperty(plugin_.DEVICE_NAME, plugin_.OFFSET_POSITION_MICRONS));
                focusPositionMicrons_.setText(core_.getProperty(plugin_.DEVICE_NAME, plugin_.FOCUS_POSITION_MICRONS));
            }
            
            val = core_.getProperty(plugin_.DEVICE_NAME, plugin_.CALCULATION_ABCD);
            String tokens[] = val.split(",");
            calculationA_.setText(tokens[0]);
            calculationB_.setText(tokens[1]);
            calculationC_.setText(tokens[2]);
            calculationD_.setText(tokens[3]);
            focusPidTarget_.setText(core_.getProperty(plugin_.DEVICE_NAME, plugin_.FOCUS_PID_TARGET));
            focusPidPosition_.setText(core_.getProperty(plugin_.DEVICE_NAME, plugin_.FOCUS_PID_POSITION));
            focusPidError_.setText(core_.getProperty(plugin_.DEVICE_NAME, plugin_.FOCUS_PID_ERROR));
            focusPidOutput_.setText(core_.getProperty(plugin_.DEVICE_NAME, plugin_.FOCUS_PID_OUTPUT));
            focusState_.setText(core_.getProperty(plugin_.DEVICE_NAME, plugin_.FOCUS_STATE));
            timeToInFocus_.setText(core_.getProperty(plugin_.DEVICE_NAME, plugin_.TIME_TO_IN_FOCUS));
            
            isOffsetMoving_.setSelected(Long.valueOf(core_.getProperty(plugin_.DEVICE_NAME, plugin_.IS_OFFSET_MOVING)) != 0);
            isFocusDriveMoving_.setSelected(Long.valueOf(core_.getProperty(plugin_.DEVICE_NAME, plugin_.IS_FOCUS_DRIVE_MOVING)) != 0);
            positiveLimitSwitch_.setSelected(Long.valueOf(core_.getProperty(plugin_.DEVICE_NAME, plugin_.POSITIVE_LIMIT_SWITCH)) != 0);
            negativeLimitSwitch_.setSelected(Long.valueOf(core_.getProperty(plugin_.DEVICE_NAME, plugin_.NEGATIVE_LIMIT_SWITCH)) != 0);
		}
		catch (Exception ex)
		{
            errored = true;
            errorMessage = ex.getMessage();
		}
        
        if (errored)
        {
            if (!errorShown_)
            {
                // Only show this once
                gui_.logs().showError(errorMessage);
            }
        }
        
        errorShown_ = errored;
	}


	/** This method is called from within the constructor to initialize the form.
	*/
	@SuppressWarnings("unchecked")
	// <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents()
	{
        setTitle("Prior PureFocus PF-850");
		setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		final JFrame frame = this;
		this.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{			
				plugin_.tellFrameClosed();
				frame.setVisible(false);
				frame.dispose();
			}
		});
        
        this.setLayout(new MigLayout("", ""));

		objectiveSelectSpinner_ = new javax.swing.JSpinner();
		objectiveSelectSpinner_.setModel(new javax.swing.SpinnerNumberModel(1, 1, 6, 1));
		objectiveSelectSpinner_.setPreferredSize(new java.awt.Dimension(100, 20));
		objectiveSelectSpinner_.addChangeListener(this);
        
        offsetPositionMicrons_ = new javax.swing.JTextField();
        offsetPositionMicrons_.setPreferredSize(new java.awt.Dimension(100, 20));
        offsetPositionMicrons_.addActionListener(this);   
        offsetPositionMicrons_.setActionCommand(plugin_.OFFSET_POSITION_MICRONS);
        
        focusPositionMicrons_ = new javax.swing.JTextField();
        focusPositionMicrons_.setPreferredSize(new java.awt.Dimension(100, 20));
        focusPositionMicrons_.addActionListener(this);   
        focusPositionMicrons_.setActionCommand(plugin_.FOCUS_POSITION_MICRONS);
        
        calculationA_ = new javax.swing.JTextField();
        calculationA_.setPreferredSize(new java.awt.Dimension(100, 20));
        calculationA_.setEditable(false);       
        
        calculationB_ = new javax.swing.JTextField();
        calculationB_.setPreferredSize(new java.awt.Dimension(100, 20));
        calculationB_.setEditable(false);       
        
        calculationC_ = new javax.swing.JTextField();
        calculationC_.setPreferredSize(new java.awt.Dimension(100, 20));
        calculationC_.setEditable(false);       
        
        calculationD_ = new javax.swing.JTextField();
        calculationD_.setPreferredSize(new java.awt.Dimension(100, 20));
        calculationD_.setEditable(false);       
        
        focusPidTarget_ = new javax.swing.JTextField();
        focusPidTarget_.setPreferredSize(new java.awt.Dimension(100, 20));
        focusPidTarget_.setEditable(false);               
        
        focusPidPosition_ = new javax.swing.JTextField();
        focusPidPosition_.setPreferredSize(new java.awt.Dimension(100, 20));
        focusPidPosition_.setEditable(false); 
        
        focusPidError_ = new javax.swing.JTextField();
        focusPidError_.setPreferredSize(new java.awt.Dimension(100, 20));
        focusPidError_.setEditable(false); 
        
        focusPidOutput_ = new javax.swing.JTextField();
        focusPidOutput_.setPreferredSize(new java.awt.Dimension(100, 20));
        focusPidOutput_.setEditable(false); 
        
        focusState_ = new javax.swing.JTextField();
        focusState_.setPreferredSize(new java.awt.Dimension(100, 20));
        focusState_.setEditable(false); 
        
        timeToInFocus_ = new javax.swing.JTextField();
        timeToInFocus_.setPreferredSize(new java.awt.Dimension(100, 20));
        timeToInFocus_.setEditable(false); 
        
        isOffsetMoving_ = new javax.swing.JCheckBox();
        isOffsetMoving_.setEnabled(false); 
        
        isFocusDriveMoving_ = new javax.swing.JCheckBox();
        isFocusDriveMoving_.setEnabled(false); 
        
        positiveLimitSwitch_ = new javax.swing.JCheckBox();
        positiveLimitSwitch_.setEnabled(false); 
        
        negativeLimitSwitch_ = new javax.swing.JCheckBox();
        negativeLimitSwitch_.setEnabled(false); 
        
        this.add(new JLabel("Objective"), "align label");
        this.add(objectiveSelectSpinner_, "wrap");
        
        this.add(new JLabel("Offset position"), "align label");
        this.add(offsetPositionMicrons_, "wrap");
        
        this.add(new JLabel("Focus position"), "align label");
        this.add(focusPositionMicrons_, "wrap");
        
        this.add(new JLabel("Calculation A"), "align label");
        this.add(calculationA_, "wrap");
        
        this.add(new JLabel("Calculation B"), "align label");
        this.add(calculationB_, "wrap");
        
        this.add(new JLabel("Calculation C"), "align label");
        this.add(calculationC_, "wrap");
        
        this.add(new JLabel("Calculation D"), "align label");
        this.add(calculationD_, "wrap");
        
        this.add(new JLabel("Focus PID target"), "align label");
        this.add(focusPidTarget_, "wrap");
        
        this.add(new JLabel("Focus PID position"), "align label");
        this.add(focusPidPosition_, "wrap");
        
         this.add(new JLabel("Focus PID error"), "align label");
        this.add(focusPidError_, "wrap");
        
        this.add(new JLabel("Focus PID output"), "align label");
        this.add(focusPidOutput_, "wrap");
        
        this.add(new JLabel("Focus state"), "align label");
        this.add(focusState_, "wrap");
        
        this.add(new JLabel("Time to in focus"), "align label");
        this.add(timeToInFocus_, "wrap");
        
        this.add(new JLabel("Is offset moving"), "align label");
        this.add(isOffsetMoving_, "wrap");
        
        this.add(new JLabel("Is focus drive moving"), "align label");
        this.add(isFocusDriveMoving_, "wrap");       
        
        this.add(new JLabel("Positive limit switch"), "align label");
        this.add(positiveLimitSwitch_, "wrap");
        
        this.add(new JLabel("Negative limit switch"), "align label");
        this.add(negativeLimitSwitch_, "wrap");      
     
		pack();
	}

    
    /** @return Name of currently-selected PureFocus */
    public String getPureFocus()
    {
        return plugin_.DEVICE_NAME;
    }
    
    
    @Override
    public void stateChanged(ChangeEvent e)
	{
        String pf = getPureFocus();            
        Object source = e.getSource();
        
		SpinnerModel numberModel = objectiveSelectSpinner_.getModel();

		int newValue = (Integer)numberModel.getValue();
		try
		{
			core_.setProperty(plugin_.DEVICE_NAME, plugin_.OBJECTIVE, newValue);
		}
		catch (Exception ex)
		{
			gui_.logs().showError(ex.getMessage());
		}        
    } 
    
    @Override
    public void actionPerformed(ActionEvent e)
	{
        String pf = getPureFocus();         
        Object source = e.getSource();
        String propertyName = e.getActionCommand();
        
        if (source.getClass() == JTextField.class)
        {
            try
            {
                JTextField widget = (JTextField)source;
                core_.setProperty(pf, propertyName, Double.valueOf(widget.getText()));
            }
            catch (Exception ex)
            {
                gui_.logs().showError(ex.getMessage());
            }       
        }
        else if (source.getClass() == Timer.class)
        {
            updateValues(false);
        }
        else
        {
            // Ignore
        }
    }    
}
