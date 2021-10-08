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
import javax.swing.JComboBox;
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


/** Child dialog for control of objective slot settings.  This is mostly intended to
 * get us started before completing the GUI, and for advanced users later.
 */
@SuppressWarnings(value = {"serial", "static-access"})  
public class PureFocusObjectiveSlotTableDialog extends JDialog implements ActionListener
{
    /** Reference to the MM/ImageJ GUI */
	private final Studio gui_;
    
    /** Reference to the frame which owns this */
	private final PureFocusFrame parent_;
	
	// GUI elements
    private JComboBox<String>[] objectivePreset_;
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
    
    /** Set true when reading values back, to block change events */
    boolean updateInProgress_;
    
    
	/** Create dialog.  This is always instantiated when the main window is
     * created, and is shown or hidden as required.
	 * @param parent Base window
	 * @param gui MM script interface
   	 */
	public PureFocusObjectiveSlotTableDialog(PureFocusFrame parent, Studio gui)
	{
        // We want this to be a modeless dialog, to work with the rest of the GUI
        super(parent, "Prior PureFocus PF-850 Objective Slots");
            
		parent_ = parent;
        gui_ = gui;
        
		initComponents();
        
        super.setIconImage(Toolkit.getDefaultToolkit().getImage(
                    getClass().getResource("/org/micromanager/icons/microscope.gif")));
        
        updateInProgress_ = false;
		updateValues(true, true, 0);      
	}
    

	/** This method is called from within the constructor to initialize the form.
	*/
	@SuppressWarnings("unchecked")
	private void initComponents()
	{
        int i;

        this.setLayout(new MigLayout("", "", ""));
        
        // Create arrays for widgets
        objectivePreset_ = new javax.swing.JComboBox[7];
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
                prefix = PureFocus.CURRENT_PREFIX;
            }
            else
            {
                prefix = PureFocus.OBJECTIVE_PREFIX + Integer.toString(i) + "-"; 
            }
            
            objectivePreset_[i] = new javax.swing.JComboBox<String>(parent_.objectivePresetNames);         
            objectivePreset_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            objectivePreset_[i].addActionListener(this);
            objectivePreset_[i].setActionCommand(prefix + PureFocus.PRESET);
            objectivePreset_[i].setEnabled(true);

            int j;
            for (j = 0; j < 5; j++)
            {            
                lensOffset_[i][j] = new javax.swing.JTextField();
                lensOffset_[i][j].setPreferredSize(new java.awt.Dimension(100, 20));
                lensOffset_[i][j].addActionListener(this);
                lensOffset_[i][j].setActionCommand(prefix + PureFocus.LENS_OFFSET + Integer.toString(j));
            }
            
            kP_[i] = new javax.swing.JTextField();
            kP_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            kP_[i].addActionListener(this);
            kP_[i].setActionCommand(prefix + PureFocus.KP);

            kI_[i] = new javax.swing.JTextField();
            kI_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            kI_[i].addActionListener(this);
            kI_[i].setActionCommand(prefix + PureFocus.KI);

            kD_[i] = new javax.swing.JTextField();
            kD_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            kD_[i].addActionListener(this);
            kD_[i].setActionCommand(prefix + PureFocus.KD);
            
            outputLimitMinimum_[i] = new javax.swing.JTextField();
            outputLimitMinimum_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            outputLimitMinimum_[i].addActionListener(this);
            outputLimitMinimum_[i].setActionCommand(prefix + PureFocus.OUTPUT_LIMIT_MINIMUM);
            
            outputLimitMaximum_[i] = new javax.swing.JTextField();
            outputLimitMaximum_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            outputLimitMaximum_[i].addActionListener(this);
            outputLimitMaximum_[i].setActionCommand(prefix + PureFocus.OUTPUT_LIMIT_MAXIMUM);
            
            sampleLowThreshold_[i] = new javax.swing.JTextField();
            sampleLowThreshold_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            sampleLowThreshold_[i].addActionListener(this);
            sampleLowThreshold_[i].setActionCommand(prefix + PureFocus.SAMPLE_LOW_THRESHOLD);
            
            focusLowThreshold_[i] = new javax.swing.JTextField();
            focusLowThreshold_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            focusLowThreshold_[i].addActionListener(this);
            focusLowThreshold_[i].setActionCommand(prefix + PureFocus.FOCUS_LOW_THRESHOLD);
            
            focusHighThreshold_[i] = new javax.swing.JTextField();
            focusHighThreshold_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            focusHighThreshold_[i].addActionListener(this);
            focusHighThreshold_[i].setActionCommand(prefix + PureFocus.FOCUS_HIGH_THRESHOLD);
            
            focusRangeThreshold_[i] = new javax.swing.JTextField();
            focusRangeThreshold_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            focusRangeThreshold_[i].addActionListener(this);           
            focusRangeThreshold_[i].setActionCommand(prefix + PureFocus.FOCUS_RANGE_THRESHOLD);
            
            interfaceHighThreshold_[i] = new javax.swing.JTextField();
            interfaceHighThreshold_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            interfaceHighThreshold_[i].addActionListener(this);
            interfaceHighThreshold_[i].setActionCommand(prefix + PureFocus.INTERFACE_HIGH_THRESHOLD);
            
            interfaceLowThreshold_[i] = new javax.swing.JTextField();
            interfaceLowThreshold_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            interfaceLowThreshold_[i].addActionListener(this);
            interfaceLowThreshold_[i].setActionCommand(prefix + PureFocus.INTERFACE_LOW_THRESHOLD);
            
            laserPower_[i] = new javax.swing.JTextField();
            laserPower_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            laserPower_[i].addActionListener(this);
            laserPower_[i].setActionCommand(prefix + PureFocus.LASER_POWER);

            backgroundA_[i] = new javax.swing.JTextField();
            backgroundA_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            backgroundA_[i].addActionListener(this);   
            backgroundA_[i].setActionCommand(prefix + PureFocus.BACKGROUND_A);
            
            backgroundB_[i] = new javax.swing.JTextField();
            backgroundB_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            backgroundB_[i].addActionListener(this);
            backgroundB_[i].setActionCommand(prefix + PureFocus.BACKGROUND_B);

            backgroundC_[i] = new javax.swing.JTextField();
            backgroundC_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            backgroundC_[i].addActionListener(this);
            backgroundC_[i].setActionCommand(prefix + PureFocus.BACKGROUND_C);

            backgroundD_[i] = new javax.swing.JTextField();
            backgroundD_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            backgroundD_[i].addActionListener(this);   
            backgroundD_[i].setActionCommand(prefix + PureFocus.BACKGROUND_D);
            
            regionStartD_[i] = new javax.swing.JTextField();
            regionStartD_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            regionStartD_[i].addActionListener(this);
            regionStartD_[i].setActionCommand(prefix + PureFocus.REGION_START_D);

            regionEndD_[i] = new javax.swing.JTextField();
            regionEndD_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            regionEndD_[i].addActionListener(this);
            regionEndD_[i].setActionCommand(prefix + PureFocus.REGION_END_D);
            
            pinholeCentre_[i] = new javax.swing.JTextField();
            pinholeCentre_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            pinholeCentre_[i].addActionListener(this);
            pinholeCentre_[i].setActionCommand(prefix + PureFocus.PINHOLE_CENTRE);

            pinholeWidth_[i] = new javax.swing.JTextField();
            pinholeWidth_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            pinholeWidth_[i].addActionListener(this);
            pinholeWidth_[i].setActionCommand(prefix + PureFocus.PINHOLE_WIDTH);
            
            isServoLimitOn_[i] = new javax.swing.JCheckBox();
            isServoLimitOn_[i].addActionListener(this);     
            isServoLimitOn_[i].setActionCommand(prefix + PureFocus.IS_SERVO_LIMIT_ON);

            servoLimitMaximumPositive_[i] = new javax.swing.JTextField();
            servoLimitMaximumPositive_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            servoLimitMaximumPositive_[i].addActionListener(this);
            servoLimitMaximumPositive_[i].setActionCommand(prefix + PureFocus.SERVO_LIMIT_MAXIMUM_POSITIVE);
            
            servoLimitMaximumNegative_[i] = new javax.swing.JTextField();
            servoLimitMaximumNegative_[i].setPreferredSize(new java.awt.Dimension(100, 20));
            servoLimitMaximumNegative_[i].addActionListener(this); 
            servoLimitMaximumNegative_[i].setActionCommand(prefix + PureFocus.SERVO_LIMIT_MAXIMUM_NEGATIVE);
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


    public void updateValues(boolean updateCurrentSlot, boolean updateAllSlots, int activateSlot)
	{
        String val;
        int i, limit;

        updateInProgress_ = true;
        
        if (updateAllSlots)
        {
            for (i = 0; i <= 6; i++)
            {
                updateSlot(i);
            }
        }
        else
        {
            // Read only current settings and selected slot
            updateSlot(0);
            updateSlot(activateSlot);
        }
        
        // Grey out other slots
        if (updateAllSlots)
        {
            for (i = 1; i <= 6; i++)
            {
                boolean slotActive = (i == activateSlot);

                objectivePreset_[i].setEnabled(slotActive);

                for (int j = 0; j < 5; j++)
                {
                    lensOffset_[i][j].setEnabled(slotActive);
                }

                kP_[i].setEnabled(slotActive);
                kI_[i].setEnabled(slotActive);
                kD_[i].setEnabled(slotActive);
                outputLimitMinimum_[i].setEnabled(slotActive);
                outputLimitMaximum_[i].setEnabled(slotActive);
                sampleLowThreshold_[i].setEnabled(slotActive);
                focusLowThreshold_[i].setEnabled(slotActive);
                focusHighThreshold_[i].setEnabled(slotActive);
                focusRangeThreshold_[i].setEnabled(slotActive);
                interfaceHighThreshold_[i].setEnabled(slotActive);
                interfaceLowThreshold_[i].setEnabled(slotActive);
                laserPower_[i].setEnabled(slotActive);
                backgroundA_[i].setEnabled(slotActive);
                backgroundB_[i].setEnabled(slotActive);
                backgroundC_[i].setEnabled(slotActive);
                backgroundD_[i].setEnabled(slotActive);
                regionStartD_[i].setEnabled(slotActive);
                regionEndD_[i].setEnabled(slotActive);
                pinholeCentre_[i].setEnabled(slotActive);
                pinholeWidth_[i].setEnabled(slotActive);
                isServoLimitOn_[i].setEnabled(slotActive);
                servoLimitMaximumPositive_[i].setEnabled(slotActive);
                servoLimitMaximumNegative_[i].setEnabled(slotActive);
            }            
        }
        
        updateInProgress_ = false;
	}
    
    
	private void updateSlot(int slot)
	{
        String pf = parent_.getPureFocus();
        CMMCore core = gui_.getCMMCore();
        
        boolean prevUpdateInProgress = updateInProgress_;
        updateInProgress_ = true;

        try
		{
            if (slot == 0)
            {
                String prefix = PureFocus.CURRENT_PREFIX;
                
                kP_[slot].setText(core.getProperty(pf, prefix + PureFocus.KP));
                kI_[slot].setText(core.getProperty(pf, prefix + PureFocus.KI));
                kD_[slot].setText(core.getProperty(pf, prefix + PureFocus.KD));    
                outputLimitMinimum_[slot].setText(core.getProperty(pf, prefix + PureFocus.OUTPUT_LIMIT_MINIMUM));
                outputLimitMaximum_[slot].setText(core.getProperty(pf, prefix + PureFocus.OUTPUT_LIMIT_MAXIMUM));
                sampleLowThreshold_[slot].setText(core.getProperty(pf, prefix + PureFocus.SAMPLE_LOW_THRESHOLD));
                focusLowThreshold_[slot].setText(core.getProperty(pf, prefix + PureFocus.FOCUS_LOW_THRESHOLD));
                focusHighThreshold_[slot].setText(core.getProperty(pf, prefix + PureFocus.FOCUS_HIGH_THRESHOLD));
                focusRangeThreshold_[slot].setText(core.getProperty(pf, prefix + PureFocus.FOCUS_RANGE_THRESHOLD));
                interfaceHighThreshold_[slot].setText(core.getProperty(pf, prefix + PureFocus.INTERFACE_HIGH_THRESHOLD));
                interfaceLowThreshold_[slot].setText(core.getProperty(pf, prefix + PureFocus.INTERFACE_LOW_THRESHOLD));
                laserPower_[slot].setText(core.getProperty(pf, prefix + PureFocus.LASER_POWER));
                backgroundA_[slot].setText(core.getProperty(pf, prefix + PureFocus.BACKGROUND_A));
                backgroundB_[slot].setText(core.getProperty(pf, prefix + PureFocus.BACKGROUND_B));
                backgroundC_[slot].setText(core.getProperty(pf, prefix + PureFocus.BACKGROUND_C));
                backgroundD_[slot].setText(core.getProperty(pf, prefix + PureFocus.BACKGROUND_D));
                regionStartD_[slot].setText(core.getProperty(pf, prefix + PureFocus.REGION_START_D));
                regionEndD_[slot].setText(core.getProperty(pf, prefix + PureFocus.REGION_END_D));
                pinholeCentre_[slot].setText(core.getProperty(pf, prefix + PureFocus.PINHOLE_CENTRE));
                pinholeWidth_[slot].setText(core.getProperty(pf, prefix + PureFocus.PINHOLE_WIDTH));
                isServoLimitOn_[slot].setSelected(Long.valueOf(core.getProperty(pf, prefix + PureFocus.IS_SERVO_LIMIT_ON)) != 0);
                servoLimitMaximumPositive_[slot].setText(core.getProperty(pf, prefix + PureFocus.SERVO_LIMIT_MAXIMUM_POSITIVE));
                servoLimitMaximumNegative_[slot].setText(core.getProperty(pf, prefix + PureFocus.SERVO_LIMIT_MAXIMUM_NEGATIVE));                
            }
            else
            {
                String prefix = PureFocus.OBJECTIVE_PREFIX + Integer.toString(slot) + "-";
                String value;
 
                value = core.getProperty(pf, prefix + PureFocus.PRESET);
                core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.PRESET, value);
                objectivePreset_[slot].setSelectedItem(value);
                
                int i;
                for (i = 0; i < 5; i ++)
                {
                    value = core.getProperty(pf, prefix + PureFocus.LENS_OFFSET + Integer.toString(i));
                    core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.LENS_OFFSET + Integer.toString(i), value);
                    lensOffset_[slot][i].setText(value);
                }
                
                value = core.getProperty(pf, prefix + PureFocus.KP);
                core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.KP, value);
                kP_[slot].setText(value);
                
                value = core.getProperty(pf, prefix + PureFocus.KI);
                core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.KI, value);
                kI_[slot].setText(value);
                
                value = core.getProperty(pf, prefix + PureFocus.KD);
                core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.KD, value);
                kD_[slot].setText(value);
                
                value = core.getProperty(pf, prefix + PureFocus.OUTPUT_LIMIT_MINIMUM);
                core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.OUTPUT_LIMIT_MINIMUM, value);
                outputLimitMinimum_[slot].setText(value);
                
                value = core.getProperty(pf, prefix + PureFocus.OUTPUT_LIMIT_MAXIMUM);
                core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.OUTPUT_LIMIT_MAXIMUM, value);
                outputLimitMaximum_[slot].setText(value);
                
                value = core.getProperty(pf, prefix + PureFocus.SAMPLE_LOW_THRESHOLD);
                core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.SAMPLE_LOW_THRESHOLD, value);
                sampleLowThreshold_[slot].setText(value);
                
                value = core.getProperty(pf, prefix + PureFocus.FOCUS_LOW_THRESHOLD);
                core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.FOCUS_LOW_THRESHOLD, value);
                focusLowThreshold_[slot].setText(value);
                
                value = core.getProperty(pf, prefix + PureFocus.FOCUS_HIGH_THRESHOLD);
                core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.FOCUS_HIGH_THRESHOLD, value);
                focusHighThreshold_[slot].setText(value);
                
                value = core.getProperty(pf, prefix + PureFocus.FOCUS_RANGE_THRESHOLD);
                core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.FOCUS_RANGE_THRESHOLD, value);
                focusRangeThreshold_[slot].setText(value);
                
                value = core.getProperty(pf, prefix + PureFocus.INTERFACE_HIGH_THRESHOLD);
                core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.INTERFACE_HIGH_THRESHOLD, value);
                interfaceHighThreshold_[slot].setText(value);
                
                value = core.getProperty(pf, prefix + PureFocus.INTERFACE_LOW_THRESHOLD);
                core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.INTERFACE_LOW_THRESHOLD, value);
                interfaceLowThreshold_[slot].setText(value);
                
                value = core.getProperty(pf, prefix + PureFocus.LASER_POWER);
                core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.LASER_POWER, value);
                laserPower_[slot].setText(value);
                
                value = core.getProperty(pf, prefix + PureFocus.BACKGROUND_A);
                core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.BACKGROUND_A, value);
                backgroundA_[slot].setText(value);
                
                value = core.getProperty(pf, prefix + PureFocus.BACKGROUND_B);
                core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.BACKGROUND_B, value);
                backgroundB_[slot].setText(value);
                
                value = core.getProperty(pf, prefix + PureFocus.BACKGROUND_C);
                core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.BACKGROUND_C, value);
                backgroundC_[slot].setText(value);
                
                value = core.getProperty(pf, prefix + PureFocus.BACKGROUND_D);
                core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.BACKGROUND_D, value);
                backgroundD_[slot].setText(value);
                
                value = core.getProperty(pf, prefix + PureFocus.REGION_START_D);
                core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.REGION_START_D, value);
                regionStartD_[slot].setText(value);
                
                value = core.getProperty(pf, prefix + PureFocus.REGION_END_D);
                core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.REGION_END_D, value);
                regionEndD_[slot].setText(value);
                
                value = core.getProperty(pf, prefix + PureFocus.PINHOLE_CENTRE);
                core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.PINHOLE_CENTRE, value);
                pinholeCentre_[slot].setText(value);
                
                value = core.getProperty(pf, prefix + PureFocus.PINHOLE_WIDTH);
                core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.PINHOLE_WIDTH, value);
                pinholeWidth_[slot].setText(value);
                
                value = core.getProperty(pf, prefix + PureFocus.IS_SERVO_LIMIT_ON);
                core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.IS_SERVO_LIMIT_ON, value);
                isServoLimitOn_[slot].setSelected(Long.valueOf(value) != 0);
                
                value = core.getProperty(pf, prefix + PureFocus.SERVO_LIMIT_MAXIMUM_POSITIVE);
                core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.SERVO_LIMIT_MAXIMUM_POSITIVE, value);
                servoLimitMaximumPositive_[slot].setText(value);
                
                value = core.getProperty(pf, prefix + PureFocus.SERVO_LIMIT_MAXIMUM_NEGATIVE);
                core.defineConfig(PureFocus.CONFIG_GROUP, PureFocus.CONFIG_GROUP_PRESET, PureFocus.DEVICE_NAME, prefix + PureFocus.SERVO_LIMIT_MAXIMUM_NEGATIVE, value);
                servoLimitMaximumNegative_[slot].setText(value);
            }
		}
		catch (Exception ex)
		{
			gui_.logs().showError(ex.getMessage());
		}
        
        updateInProgress_ = prevUpdateInProgress;
	}
    
    
    @Override
    public void actionPerformed(ActionEvent e)
	{
        String pf = parent_.getPureFocus();
        CMMCore core = gui_.getCMMCore();            
        Object source = e.getSource();
        String propertyName = e.getActionCommand();
        
        if (!updateInProgress_)
        {
            try
            {
                int slot = 0;
                core.setProperty(PureFocus.DEVICE_NAME, PureFocus.SINGLE_CHANGE_IN_PROGRESS, 1);

                // Work out slot number from property name
                int slotStart = propertyName.indexOf(PureFocus.OBJECTIVE_PREFIX);
                if (slotStart >= 0)
                {
                    int index = slotStart + PureFocus.OBJECTIVE_PREFIX.length();
                    slot = Integer.valueOf(propertyName.substring(index, index + 1));
                }

                if (source.getClass() == JTextField.class)
                {
                    JTextField widget = (JTextField)source;
                    String val = widget.getText();
                    Double value = Double.valueOf(val);
                    core.setProperty(pf, propertyName, value);
                }
                else if (source.getClass() == JComboBox.class)
                {
                    JComboBox<String> widget = (JComboBox<String>)source;
                    String val = (String)widget.getSelectedItem();               
                    core.setProperty(pf, propertyName, val);
                }
                else if (source.getClass() == JCheckBox.class)
                {
                    JCheckBox widget = (JCheckBox)source;
                    if (widget.isSelected())
                    {
                        core.setProperty(pf, propertyName, 1);
                    }
                    else
                    {
                        core.setProperty(pf, propertyName, 0);                          
                    }            
                }
                else
                {
                    // Unknown so ignore it
                }

                core.setProperty(PureFocus.DEVICE_NAME, PureFocus.SINGLE_CHANGE_IN_PROGRESS, 0);

                if (slot != 0)
                {
                    // Update all settings for this slot
                    updateSlot(slot);
                }

                // Update current settings
                updateSlot(0);
            }
            catch (Exception ex)
            {
                // All exceptions need the same basic response
                try
                {
                    // Ensure PureFocus is not left open for changes
                    core.setProperty(PureFocus.DEVICE_NAME, PureFocus.SINGLE_CHANGE_IN_PROGRESS, 0);
                }
                catch (Exception e2)
                {
                    // These actions should not be able to fail
                }
                
                try
                {                
                    // If something went wrong, update widget value
                    if (source.getClass() == JTextField.class)
                    {
                        JTextField widget = (JTextField)source;
                        widget.setText(core.getProperty(pf, propertyName));
                    }
                    else if (source.getClass() == JComboBox.class)
                    {
                        JComboBox<String> widget = (JComboBox<String>)source;
                        widget.setSelectedItem(core.getProperty(pf, propertyName));
                    }                    
                    else if (source.getClass() == JCheckBox.class)
                    {
                        JCheckBox widget = (JCheckBox)source;
                        widget.setSelected(Integer.valueOf(core.getProperty(pf, propertyName)) != 0);
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

                if (ex.getClass() == NumberFormatException.class)
                {
                    gui_.logs().showError("Value is not a number");
                }
                else
                {
                    gui_.logs().showError(ex.getMessage());
                }
            }
        }
	}
}
