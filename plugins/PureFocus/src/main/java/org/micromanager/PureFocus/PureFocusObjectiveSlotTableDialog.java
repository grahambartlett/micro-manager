/**
@file PureFocusObjectiveSlotTableFrame.java
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
import javax.swing.JDialog;
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


/** Micro-Manager plugin for control of the Prior PureFocus PF-850 autofocus 
 */
@SuppressWarnings(value = {"serial", "static-access"})  
public class PureFocusObjectiveSlotTableDialog extends JDialog implements ActionListener
{
    private final PureFocus plugin_;
	private final PureFocusFrame parent_;
    private final Studio gui_;
	
	// GUI elements
    private JTextField[] objectivePreset_;
    private JTextField[][] lensOffset_;
    private JTextField[] kP_;
    private JTextField[] kI_;
    private JTextField[] kD_;
    private JTextField[] outputLimitMinimum_;
    private JTextField[] outputLimitMaximum_;    
    private JTextField[] sampleLowThreshold_;
    private JTextField[] focusLowThreshold_;
    private JTextField[] focusHighThreshold_;
    private JTextField[] focusRangeThreshold_;  
    private JTextField[] interfaceHighThreshold_;
    private JTextField[] interfaceLowThreshold_;
    private JTextField[] laserPower_;
    private JTextField[] backgroundA_;
    private JTextField[] backgroundB_;
    private JTextField[] backgroundC_;
    private JTextField[] backgroundD_;
    private JTextField[] regionStartD_;
    private JTextField[] regionEndD_;
    private JTextField[] pinholeCentre_;
    private JTextField[] pinholeWidth_;    
    private JCheckBox[] isServoLimitOn_;
    private JTextField[] servoLimitMaximumPositive_;
    private JTextField[] servoLimitMaximumNegative_;
    

	/** Creates new form PureFocusObjectiveSlotTableFrame
	@param parent Base window
    @param plugin PureFocus plugin
	@param gui MM scriptInterface
	*/
	public PureFocusObjectiveSlotTableDialog(PureFocusFrame parent, PureFocus plugin, Studio gui)
	{
        // We want this to be a modeless dialog, to work with the rest of the GUI
        super(parent, "Prior PureFocus PF-850 Objective Slots");
            
		parent_ = parent;
        plugin_ = plugin;
        gui_ = gui;
        
		initComponents();
        
        super.setIconImage(Toolkit.getDefaultToolkit().getImage(
                    getClass().getResource("/org/micromanager/icons/microscope.gif")));
        
		updateValues(true);
	}
    

	private void updateValues(boolean allValues)
	{
        String val;
        int i, limit;

        if (allValues)
        {
            for (i = 0; i <= 6; i++)
            {
                updateSlot(i);
            }
        }
        else
        {
            // Read only current settings
            updateSlot(0);
        }
	}
    
    
	private void updateSlot(int slot)
	{
        String pf = parent_.getPureFocus();
        CMMCore core = gui_.getCMMCore();

        try
		{
            String prefix;
            if (slot == 0)
            {
                prefix = plugin_.CURRENT_PREFIX;
            }
            else
            {
                prefix = plugin_.OBJECTIVE_PREFIX + Integer.toString(slot) + "-";
            }

            if (slot != 0)
            {
                objectivePreset_[slot].setText(core.getProperty(pf, prefix + plugin_.PRESET));
                
                int i;
                for (i = 0; i < 5; i ++)
                {
                    lensOffset_[slot][i].setText(core.getProperty(pf, prefix + plugin_.LENS_OFFSET + Integer.toString(i)));
                }
            }            

            kP_[slot].setText(core.getProperty(pf, prefix + plugin_.KP));
            kI_[slot].setText(core.getProperty(pf, prefix + plugin_.KI));
            kD_[slot].setText(core.getProperty(pf, prefix + plugin_.KD));    
            outputLimitMinimum_[slot].setText(core.getProperty(pf, prefix + plugin_.OUTPUT_LIMIT_MINIMUM));
            outputLimitMaximum_[slot].setText(core.getProperty(pf, prefix + plugin_.OUTPUT_LIMIT_MAXIMUM));
            sampleLowThreshold_[slot].setText(core.getProperty(pf, prefix + plugin_.SAMPLE_LOW_THRESHOLD));
            focusLowThreshold_[slot].setText(core.getProperty(pf, prefix + plugin_.FOCUS_LOW_THRESHOLD));
            focusHighThreshold_[slot].setText(core.getProperty(pf, prefix + plugin_.FOCUS_HIGH_THRESHOLD));
            focusRangeThreshold_[slot].setText(core.getProperty(pf, prefix + plugin_.FOCUS_RANGE_THRESHOLD));
            interfaceHighThreshold_[slot].setText(core.getProperty(pf, prefix + plugin_.INTERFACE_HIGH_THRESHOLD));
            interfaceLowThreshold_[slot].setText(core.getProperty(pf, prefix + plugin_.INTERFACE_LOW_THRESHOLD));
            laserPower_[slot].setText(core.getProperty(pf, prefix + plugin_.LASER_POWER));
            backgroundA_[slot].setText(core.getProperty(pf, prefix + plugin_.BACKGROUND_A));
            backgroundB_[slot].setText(core.getProperty(pf, prefix + plugin_.BACKGROUND_B));
            backgroundC_[slot].setText(core.getProperty(pf, prefix + plugin_.BACKGROUND_C));
            backgroundD_[slot].setText(core.getProperty(pf, prefix + plugin_.BACKGROUND_D));
            regionStartD_[slot].setText(core.getProperty(pf, prefix + plugin_.REGION_START_D));
            regionEndD_[slot].setText(core.getProperty(pf, prefix + plugin_.REGION_END_D));
            pinholeCentre_[slot].setText(core.getProperty(pf, prefix + plugin_.PINHOLE_CENTRE));
            pinholeWidth_[slot].setText(core.getProperty(pf, prefix + plugin_.PINHOLE_WIDTH));
            isServoLimitOn_[slot].setSelected(Long.valueOf(core.getProperty(pf, prefix + plugin_.IS_SERVO_LIMIT_ON)) != 0);
            servoLimitMaximumPositive_[slot].setText(core.getProperty(pf, prefix + plugin_.SERVO_LIMIT_MAXIMUM_POSITIVE));
            servoLimitMaximumNegative_[slot].setText(core.getProperty(pf, prefix + plugin_.SERVO_LIMIT_MAXIMUM_NEGATIVE));
		}
		catch (Exception ex)
		{
			gui_.logs().showError("Error reading values from Prior PureFocus PF-850");
		}
	}    


	/** This method is called from within the constructor to initialize the form.
	*/
	@SuppressWarnings("unchecked")
	private void initComponents()
	{
        int i;

        this.setLayout(new MigLayout("", "", ""));

        // Create arrays for widgets
        objectivePreset_ = new javax.swing.JTextField[7];
        lensOffset_ = new javax.swing.JTextField[7][5];
        kP_ = new javax.swing.JTextField[7];
        kI_ = new javax.swing.JTextField[7];
        kD_ = new javax.swing.JTextField[7];
        outputLimitMinimum_ = new javax.swing.JTextField[7];
        outputLimitMaximum_ = new javax.swing.JTextField[7];
        sampleLowThreshold_ = new javax.swing.JTextField[7];
        focusLowThreshold_ = new javax.swing.JTextField[7];
        focusHighThreshold_ = new javax.swing.JTextField[7];
        focusRangeThreshold_ = new javax.swing.JTextField[7];
        interfaceHighThreshold_ = new javax.swing.JTextField[7];
        interfaceLowThreshold_ = new javax.swing.JTextField[7];
        laserPower_ = new javax.swing.JTextField[7];
        backgroundA_ = new javax.swing.JTextField[7];
        backgroundB_ = new javax.swing.JTextField[7];
        backgroundC_ = new javax.swing.JTextField[7];
        backgroundD_ = new javax.swing.JTextField[7];       
        regionStartD_ = new javax.swing.JTextField[7];  
        regionEndD_ = new javax.swing.JTextField[7];
        pinholeCentre_ = new javax.swing.JTextField[7];
        pinholeWidth_ = new javax.swing.JTextField[7];
        isServoLimitOn_ = new javax.swing.JCheckBox[7];
        servoLimitMaximumPositive_ = new javax.swing.JTextField[7];
        servoLimitMaximumNegative_ = new javax.swing.JTextField[7];
        
        // Create widgets
        for (i = 0; i <= 6; i++)
        {
            String prefix;
            if (i == 0)
            {
                prefix = plugin_.CURRENT_PREFIX;
            }
            else
            {
                prefix = plugin_.OBJECTIVE_PREFIX + Integer.toString(i) + "-"; 
            }
            
            objectivePreset_[i] = new javax.swing.JTextField();
            objectivePreset_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            objectivePreset_[i].addActionListener(this);
            
            int j;
            for (j = 0; j < 5; j++)
            {            
                lensOffset_[i][j] = new javax.swing.JTextField();
                lensOffset_[i][j].setPreferredSize(new java.awt.Dimension(100, 20));
                lensOffset_[i][j].addActionListener(this);                
            }
            
            kP_[i] = new javax.swing.JTextField();
            kP_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            kP_[i].addActionListener(this);

            kI_[i] = new javax.swing.JTextField();
            kI_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            kI_[i].addActionListener(this);

            kD_[i] = new javax.swing.JTextField();
            kD_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            kD_[i].addActionListener(this);
            
            outputLimitMinimum_[i] = new javax.swing.JTextField();
            outputLimitMinimum_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            outputLimitMinimum_[i].addActionListener(this);
            
            outputLimitMaximum_[i] = new javax.swing.JTextField();
            outputLimitMaximum_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            outputLimitMaximum_[i].addActionListener(this);
            
            sampleLowThreshold_[i] = new javax.swing.JTextField();
            sampleLowThreshold_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            sampleLowThreshold_[i].addActionListener(this);
            
            focusLowThreshold_[i] = new javax.swing.JTextField();
            focusLowThreshold_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            focusLowThreshold_[i].addActionListener(this);
            
            focusHighThreshold_[i] = new javax.swing.JTextField();
            focusHighThreshold_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            focusHighThreshold_[i].addActionListener(this);
            
            focusRangeThreshold_[i] = new javax.swing.JTextField();
            focusRangeThreshold_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            focusRangeThreshold_[i].addActionListener(this);           
            
            interfaceHighThreshold_[i] = new javax.swing.JTextField();
            interfaceHighThreshold_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            interfaceHighThreshold_[i].addActionListener(this);
            
            interfaceLowThreshold_[i] = new javax.swing.JTextField();
            interfaceLowThreshold_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            interfaceLowThreshold_[i].addActionListener(this);
            
            laserPower_[i] = new javax.swing.JTextField();
            laserPower_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            laserPower_[i].addActionListener(this);

            backgroundA_[i] = new javax.swing.JTextField();
            backgroundA_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            backgroundA_[i].addActionListener(this);   
            
            backgroundB_[i] = new javax.swing.JTextField();
            backgroundB_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            backgroundB_[i].addActionListener(this);

            backgroundC_[i] = new javax.swing.JTextField();
            backgroundC_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            backgroundC_[i].addActionListener(this);

            backgroundD_[i] = new javax.swing.JTextField();
            backgroundD_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            backgroundD_[i].addActionListener(this);          
            
            regionStartD_[i] = new javax.swing.JTextField();
            regionStartD_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            regionStartD_[i].addActionListener(this);

            regionEndD_[i] = new javax.swing.JTextField();
            regionEndD_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            regionEndD_[i].addActionListener(this);
            
            pinholeCentre_[i] = new javax.swing.JTextField();
            pinholeCentre_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            pinholeCentre_[i].addActionListener(this);

            pinholeWidth_[i] = new javax.swing.JTextField();
            pinholeWidth_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            pinholeWidth_[i].addActionListener(this);
            
            isServoLimitOn_[i] = new javax.swing.JCheckBox();
            isServoLimitOn_[i].addActionListener(this);            

            servoLimitMaximumPositive_[i] = new javax.swing.JTextField();
            servoLimitMaximumPositive_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            servoLimitMaximumPositive_[i].addActionListener(this);
            
            servoLimitMaximumNegative_[i] = new javax.swing.JTextField();
            servoLimitMaximumNegative_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            servoLimitMaximumNegative_[i].addActionListener(this);        
        }
        
        // All current properties are read-only
        kP_[0].setEditable(false);
        kI_[0].setEditable(false);
        kD_[0].setEditable(false);          
        outputLimitMinimum_[0].setEditable(false);
        outputLimitMaximum_[0].setEditable(false);
        sampleLowThreshold_[0].setEditable(false);
        focusLowThreshold_[0].setEditable(false);
        focusHighThreshold_[0].setEditable(false);
        focusRangeThreshold_[0].setEditable(false);
        interfaceHighThreshold_[0].setEditable(false);
        interfaceLowThreshold_[0].setEditable(false);
        laserPower_[0].setEditable(false);
        backgroundA_[0].setEditable(false);
        backgroundB_[0].setEditable(false);
        backgroundC_[0].setEditable(false);
        backgroundD_[0].setEditable(false);
        regionStartD_[0].setEditable(false);
        regionEndD_[0].setEditable(false);
        pinholeCentre_[0].setEditable(false);
        pinholeWidth_[0].setEditable(false);
        isServoLimitOn_[0].setEnabled(false);
        servoLimitMaximumPositive_[0].setEditable(false);
        servoLimitMaximumNegative_[0].setEditable(false);
        
        // Add to layout
        this.add(new JLabel(""), "align label");
        this.add(new JLabel("Current"), "align");
        this.add(new JLabel("Objective 1"), "align");
        this.add(new JLabel("Objective 2"), "align");
        this.add(new JLabel("Objective 3"), "align");
        this.add(new JLabel("Objective 4"), "align");
        this.add(new JLabel("Objective 5"), "align");
        this.add(new JLabel("Objective 6"), "wrap");
        
        this.add(new JLabel("Preset"), "align label");
        this.add(new JLabel(""), "align");
        for (i = 1; i < 6; i++)
        {
            this.add(objectivePreset_[i], "align");
        }
        this.add(objectivePreset_[6], "wrap");
        
        this.add(new JLabel("Default lens offset"), "align label");
        this.add(new JLabel(""), "align");
        for (i = 1; i < 6; i++)
        {
            this.add(lensOffset_[i][0], "align");
        }
        this.add(lensOffset_[6][0], "wrap");    

        this.add(new JLabel("Lens offset 1"), "align label");
        this.add(new JLabel(""), "align");
        for (i = 1; i < 6; i++)
        {
            this.add(lensOffset_[i][1], "align");
        }
        this.add(lensOffset_[6][1], "wrap");          
        
        this.add(new JLabel("Lens offset 2"), "align label");
        this.add(new JLabel(""), "align");
        for (i = 1; i < 6; i++)
        {
            this.add(lensOffset_[i][2], "align");
        }
        this.add(lensOffset_[6][2], "wrap");  
        
        this.add(new JLabel("Lens offset 3"), "align label");
        this.add(new JLabel(""), "align");
        for (i = 1; i < 6; i++)
        {
            this.add(lensOffset_[i][3], "align");
        }
        this.add(lensOffset_[6][3], "wrap");  
        
        this.add(new JLabel("Lens offset 4"), "align label");
        this.add(new JLabel(""), "align");
        for (i = 1; i < 6; i++)
        {
            this.add(lensOffset_[i][4], "align");
        }
        this.add(lensOffset_[6][4], "wrap");  
        
        this.add(new JLabel("KP"), "align label");
        for (i = 0; i < 6; i++)
        {
            this.add(kP_[i], "align");
        }
        this.add(kP_[6], "wrap");
        
        this.add(new JLabel("KI"), "align label");
        for (i = 0; i < 6; i++)
        {
            this.add(kI_[i], "align");
        }
        this.add(kI_[6], "wrap");
        
        this.add(new JLabel("KD"), "align label");
        for (i = 0; i < 6; i++)
        {
            this.add(kD_[i], "align");
        }
        this.add(kD_[6], "wrap");
        
        this.add(new JLabel("Output limit min"), "align label");
        for (i = 0; i < 6; i++)
        {
            this.add(outputLimitMinimum_[i], "align");
        }
        this.add(outputLimitMinimum_[6], "wrap");
        
        this.add(new JLabel("Output limit max"), "align label");
        for (i = 0; i < 6; i++)
        {
            this.add(outputLimitMaximum_[i], "align");
        }
        this.add(outputLimitMaximum_[6], "wrap");       
        
        this.add(new JLabel("Sample low threshold"), "align label");
        for (i = 0; i < 6; i++)
        {
            this.add(sampleLowThreshold_[i], "align");
        }
        this.add(sampleLowThreshold_[6], "wrap");
        
        this.add(new JLabel("Sample high threshold"), "align label");
        for (i = 0; i < 6; i++)
        {
            this.add(focusLowThreshold_[i], "align");
        }
        this.add(focusLowThreshold_[6], "wrap");
        
        this.add(new JLabel("Focus high threshold"), "align label");
        for (i = 0; i < 6; i++)
        {
            this.add(focusHighThreshold_[i], "align");
        }
        this.add(focusHighThreshold_[6], "wrap");     
         
        this.add(new JLabel("Focus range threshold"), "align label");
        for (i = 0; i < 6; i++)
        {
            this.add(focusRangeThreshold_[i], "align");
        }
        this.add(focusRangeThreshold_[6], "wrap");

        this.add(new JLabel("Interface high threshold"), "align label");
        for (i = 0; i < 6; i++)
        {
            this.add(interfaceHighThreshold_[i], "align");
        }
        this.add(interfaceHighThreshold_[6], "wrap");
        
        this.add(new JLabel("Interface low threshold"), "align label");
        for (i = 0; i < 6; i++)
        {
            this.add(interfaceLowThreshold_[i], "align");
        }
        this.add(interfaceLowThreshold_[6], "wrap");
        
        this.add(new JLabel("Laser power"), "align label");
        for (i = 0; i < 6; i++)
        {
            this.add(laserPower_[i], "align");
        }
        this.add(laserPower_[6], "wrap");
        
        this.add(new JLabel("Background A"), "align label");
        for (i = 0; i < 6; i++)
        {
            this.add(backgroundA_[i], "align");
        }
        this.add(backgroundA_[6], "wrap");
        
        this.add(new JLabel("Background B"), "align label");
        for (i = 0; i < 6; i++)
        {
            this.add(backgroundB_[i], "align");
        }
        this.add(backgroundB_[6], "wrap");
        
        this.add(new JLabel("Background C"), "align label");
        for (i = 0; i < 6; i++)
        {
            this.add(backgroundC_[i], "align");
        }
        this.add(backgroundC_[6], "wrap");
        
        this.add(new JLabel("Background D"), "align label");
        for (i = 0; i < 6; i++)
        {
            this.add(backgroundD_[i], "align");
        }
        this.add(backgroundD_[6], "wrap");
        
        this.add(new JLabel("Region start D"), "align label");
        for (i = 0; i < 6; i++)
        {
            this.add(regionStartD_[i], "align");
        }
        this.add(regionStartD_[6], "wrap");
        
        this.add(new JLabel("Region end D"), "align label");
        for (i = 0; i < 6; i++)
        {
            this.add(regionEndD_[i], "align");
        }
        this.add(regionEndD_[6], "wrap");        
        
        this.add(new JLabel("Pinhole centre"), "align label");
        for (i = 0; i < 6; i++)
        {
            this.add(pinholeCentre_[i], "align");
        }
        this.add(pinholeCentre_[6], "wrap");
        
        this.add(new JLabel("Pinhole width"), "align label");
        for (i = 0; i < 6; i++)
        {
            this.add(pinholeWidth_[i], "align");
        }
        this.add(pinholeWidth_[6], "wrap");
        
        this.add(new JLabel("Is servo limit on"), "align label");
        for (i = 0; i < 6; i++)
        {
            this.add(isServoLimitOn_[i], "align");
        }
        this.add(isServoLimitOn_[6], "wrap");

        this.add(new JLabel("Servo limit max positive"), "align label");
        for (i = 0; i < 6; i++)
        {
            this.add(servoLimitMaximumPositive_[i], "align");
        }
        this.add(servoLimitMaximumPositive_[6], "wrap");
        
        this.add(new JLabel("Servo limit max negative"), "align label");
        for (i = 0; i < 6; i++)
        {
            this.add(servoLimitMaximumNegative_[i], "align");
        }
        this.add(servoLimitMaximumNegative_[6], "wrap");
        
        pack();
	}

				
    @Override
    public void actionPerformed(ActionEvent e)
	{
        String pf = parent_.getPureFocus();
        CMMCore core = gui_.getCMMCore();            
        Object source = e.getSource();
        
		try
		{
            int slot = 0;
            core.setProperty(plugin_.DEVICE_NAME, plugin_.SINGLE_CHANGE_IN_PROGRESS, 1);
            
            if (source.getClass() == JTextField.class)
            {
                JTextField widget = (JTextField)source;
                int i = 0;

                for (i = 1; i <= 6; i++)
                {
                    String prefix = plugin_.OBJECTIVE_PREFIX + Integer.toString(i) + "-";

                    if (source == objectivePreset_[i])
                    {
                        String val = widget.getText();
                        core.setProperty(pf, prefix + plugin_.PRESET, val);
                        break;
                    }
                    
                    if (source == lensOffset_[i][0])
                    {
                        String val = widget.getText();
                        core.setProperty(pf, prefix + plugin_.LENS_OFFSET + "0", val);
                        break;
                    }
                    
                    if (source == lensOffset_[i][1])
                    {
                        String val = widget.getText();
                        core.setProperty(pf, prefix + plugin_.LENS_OFFSET + "1", val);
                        break;
                    }
                    
                    if (source == lensOffset_[i][2])
                    {
                        String val = widget.getText();
                        core.setProperty(pf, prefix + plugin_.LENS_OFFSET + "2", val);
                        break;
                    }
                    
                    if (source == lensOffset_[i][3])
                    {
                        String val = widget.getText();
                        core.setProperty(pf, prefix + plugin_.LENS_OFFSET + "3", val);
                        break;
                    }
                    
                    if (source == lensOffset_[i][4])
                    {
                        String val = widget.getText();
                        core.setProperty(pf, prefix + plugin_.LENS_OFFSET + "4", val);
                        break;
                    }
 
                    if (source == kP_[i])
                    {
                        String val = widget.getText();
                        Double value = Double.valueOf(val);
                        core.setProperty(pf, prefix + plugin_.KP, value);
                        break;
                    }

                    if (source == kI_[i])
                    {
                        String val = widget.getText();
                        Double value = Double.valueOf(val);
                        core.setProperty(pf, prefix + plugin_.KI, value);
                        break;
                    }                

                    if (source == kD_[i])
                    {
                        String val = widget.getText();
                        Double value = Double.valueOf(val);
                        core.setProperty(pf, prefix + plugin_.KD, value);
                        break;
                    }

                    if (source == outputLimitMinimum_[i])
                    {
                        String val = widget.getText();
                        Double value = Double.valueOf(val);
                        core.setProperty(pf, prefix + plugin_.OUTPUT_LIMIT_MINIMUM, value);
                        break;
                    }

                    if (source == outputLimitMaximum_[i])
                    {
                        String val = widget.getText();
                        Double value = Double.valueOf(val);
                        core.setProperty(pf, prefix + plugin_.OUTPUT_LIMIT_MAXIMUM, value);
                        break;
                    }                

                    if (source == sampleLowThreshold_[i])
                    {
                        String val = widget.getText();
                        Double value = Double.valueOf(val);
                        core.setProperty(pf, prefix + plugin_.SAMPLE_LOW_THRESHOLD, value);
                        break;
                    }
    
                    if (source == focusLowThreshold_[i])
                    {
                        String val = widget.getText();
                        Double value = Double.valueOf(val);
                        core.setProperty(pf, prefix + plugin_.FOCUS_LOW_THRESHOLD, value);
                        break;
                    }

                    if (source == focusHighThreshold_[i])
                    {
                        String val = widget.getText();
                        Double value = Double.valueOf(val);
                        core.setProperty(pf, prefix + plugin_.FOCUS_HIGH_THRESHOLD, value);
                        break;
                    }                

                    if (source == focusRangeThreshold_[i])
                    {
                        String val = widget.getText();
                        Double value = Double.valueOf(val);
                        core.setProperty(pf, prefix + plugin_.FOCUS_RANGE_THRESHOLD, value);
                        break;
                    }                    

                    if (source == interfaceHighThreshold_[i])
                    {
                        String val = widget.getText();
                        Double value = Double.valueOf(val);
                        core.setProperty(pf, prefix + plugin_.INTERFACE_HIGH_THRESHOLD, value);
                        break;
                    }                

                    if (source == interfaceLowThreshold_[i])
                    {
                        String val = widget.getText();
                        Double value = Double.valueOf(val);
                        core.setProperty(pf, prefix + plugin_.INTERFACE_LOW_THRESHOLD, value);
                        break;
                    }

                   if (source == laserPower_[i])
                    {
                        String val = widget.getText();
                        Double value = Double.valueOf(val);
                        core.setProperty(pf, prefix + plugin_.LASER_POWER, value);
                        break;
                    }

                    if (source == backgroundA_[i])
                    {
                        String val = widget.getText();
                        Double value = Double.valueOf(val);
                        core.setProperty(pf, prefix + plugin_.BACKGROUND_A, value);
                        break;
                    }

                    if (source == backgroundB_[i])
                    {
                        String val = widget.getText();
                        Double value = Double.valueOf(val);
                        core.setProperty(pf, prefix + plugin_.BACKGROUND_B, value);
                        break;
                    }

                    if (source == backgroundC_[i])
                    {
                        String val = widget.getText();
                        Double value = Double.valueOf(val);
                        core.setProperty(pf, prefix + plugin_.BACKGROUND_C, value);
                        break;
                    }

                    if (source == backgroundD_[i])
                    {
                        String val = widget.getText();
                        Double value = Double.valueOf(val);
                        core.setProperty(pf, prefix + plugin_.BACKGROUND_D, value);
                        break;
                    }
                    
                    if (source == regionStartD_[i])
                    {
                        String val = widget.getText();
                        Double value = Double.valueOf(val);
                        core.setProperty(pf, prefix + plugin_.REGION_START_D, value);
                        break;
                    }
                    
                    if (source == regionEndD_[i])
                    {
                        String val = widget.getText();
                        Double value = Double.valueOf(val);
                        core.setProperty(pf, prefix + plugin_.REGION_END_D, value);
                        break;
                    }

                    if (source == pinholeCentre_[i])
                    {
                        String val = widget.getText();
                        Double value = Double.valueOf(val);
                        core.setProperty(pf, prefix + plugin_.PINHOLE_CENTRE, value);
                        break;
                    }                    

                    if (source == pinholeWidth_[i])
                    {
                        String val = widget.getText();
                        Double value = Double.valueOf(val);
                        core.setProperty(pf, prefix + plugin_.PINHOLE_WIDTH, value);
                        break;
                    }

                    if (source == servoLimitMaximumPositive_[i])
                    {
                        String val = widget.getText();
                        Double value = Double.valueOf(val);
                        core.setProperty(pf, prefix + plugin_.SERVO_LIMIT_MAXIMUM_POSITIVE, value);
                        break;
                    }

                    if (source == servoLimitMaximumNegative_[i])
                    {
                        String val = widget.getText();
                        Double value = Double.valueOf(val);
                        core.setProperty(pf, prefix + plugin_.SERVO_LIMIT_MAXIMUM_NEGATIVE, value);
                        break;
                    }                            
                }
                
                if (i <= 6)
                {
                    slot = i;
                }
            }
            else if (source.getClass() == JCheckBox.class)
            {
                JCheckBox widget = (JCheckBox)source;
                int i;

                for (i = 1; i <= 6; i++)
                {
                    String prefix = plugin_.OBJECTIVE_PREFIX + Integer.toString(i) + "-";

                    if (source == isServoLimitOn_[i])
                    {
                        Integer value;
                        if (widget.isSelected())
                        {
                            value = 1;
                        }
                        else
                        {
                            value = 0;                           
                        }
                        core.setProperty(pf, prefix + plugin_.IS_SERVO_LIMIT_ON, value);
                        break;
                    }
                }
                     
                if (i <= 6)
                {
                    slot = i;
                }                
            }
            else
            {
                // Unknown so ignore it
            }
                
            core.setProperty(plugin_.DEVICE_NAME, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
            
            if (slot != 0)
            {
                // Update all settings for this slot
                updateSlot(slot);
            }
            
            // Update current settings
            updateSlot(0);
            
            core.updateCoreProperties();
            core.updateSystemStateCache();
    	}
		catch (Exception ex)
		{
            try
            {
                // Ensure PureFocus is not left open for changes
                core.setProperty(plugin_.DEVICE_NAME, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
                
                // If something went wrong, update widget value
                if (source.getClass() == JTextField.class)
                {
                    JTextField widget = (JTextField)source;
                    int i;

                    for (i = 1; i <= 6; i++)
                    {
                        String prefix = plugin_.OBJECTIVE_PREFIX + Integer.toString(i) + "-";

                        if (source == objectivePreset_[i])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.PRESET));
                            break;
                        }
                        
                        if (source == lensOffset_[i][0])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.LENS_OFFSET + "0"));
                            break;
                        }
                        
                        if (source == lensOffset_[i][1])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.LENS_OFFSET + "1"));
                            break;
                        }  
                        
                        if (source == lensOffset_[i][2])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.LENS_OFFSET + "2"));
                            break;
                        }  
                        
                        if (source == lensOffset_[i][3])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.LENS_OFFSET + "3"));
                            break;
                        }  
                        
                        if (source == lensOffset_[i][4])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.LENS_OFFSET + "4"));
                            break;
                        } 
                        
                        if (source == kP_[i])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.KP));
                            break;
                        }

                        if (source == kI_[i])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.KI));
                            break;
                        }                

                        if (source == kD_[i])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.KD));
                            break;
                        }
    
                        if (source == outputLimitMinimum_[i])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.OUTPUT_LIMIT_MINIMUM));
                            break;
                        }

                        if (source == outputLimitMaximum_[i])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.OUTPUT_LIMIT_MAXIMUM));
                            break;
                        }                

                        if (source == sampleLowThreshold_[i])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.SAMPLE_LOW_THRESHOLD));
                            break;
                        }
    
                        if (source == focusLowThreshold_[i])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.FOCUS_LOW_THRESHOLD));
                            break;
                        }

                        if (source == focusHighThreshold_[i])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.FOCUS_HIGH_THRESHOLD));
                            break;
                        }                

                        if (source == focusRangeThreshold_[i])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.FOCUS_RANGE_THRESHOLD));
                            break;
                        }                        
                        
                        if (source == interfaceHighThreshold_[i])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.INTERFACE_HIGH_THRESHOLD));
                            break;
                        }                

                        if (source == interfaceLowThreshold_[i])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.INTERFACE_LOW_THRESHOLD));
                            break;
                        }

                        if (source == laserPower_[i])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.LASER_POWER));
                            break;
                        }

                        if (source == backgroundA_[i])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.BACKGROUND_A));
                            break;
                        }

                        if (source == backgroundB_[i])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.BACKGROUND_B));
                            break;
                        }

                        if (source == backgroundC_[i])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.BACKGROUND_C));
                            break;
                        }

                        if (source == backgroundD_[i])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.BACKGROUND_D));
                            break;
                        }

                        if (source == regionStartD_[i])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.REGION_START_D));
                            break;
                        }

                        if (source == regionEndD_[i])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.REGION_END_D));
                            break;
                        }                                             

                        if (source == pinholeCentre_[i])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.PINHOLE_CENTRE));
                            break;
                        }                    

                        if (source == pinholeWidth_[i])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.PINHOLE_CENTRE));
                            break;
                        }

                        if (source == servoLimitMaximumPositive_[i])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.SERVO_LIMIT_MAXIMUM_POSITIVE));
                            break;
                        }
    
                        if (source == servoLimitMaximumNegative_[i])
                        {
                            widget.setText(core.getProperty(pf, prefix + plugin_.SERVO_LIMIT_MAXIMUM_NEGATIVE));
                            break;
                        }             
                    }
                }
                else if (source.getClass() == JCheckBox.class)
                {
                    JCheckBox widget = (JCheckBox)source;
                    int i;

                    for (i = 1; i <= 6; i++)
                    {
                        String prefix = plugin_.OBJECTIVE_PREFIX + Integer.toString(i) + "-";

                        if (source == isServoLimitOn_[i])
                        {
                            String value = core.getProperty(pf, prefix + plugin_.IS_SERVO_LIMIT_ON);
                            Integer val = Integer.valueOf(value);
                            boolean valBool = (val != 0);
                            widget.setSelected(valBool);
                            break;
                        }
                    }
                }
                else
                {
                    // Unknown so ignore it
                }
            }
            catch (Exception e2)
            {
                // These actions should not be able to fail
            }
        }
	}
}
