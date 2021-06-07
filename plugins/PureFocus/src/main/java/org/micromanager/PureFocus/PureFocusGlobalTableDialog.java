/**
@file PureFocusGlobalTableDialog.java
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
public class PureFocusGlobalTableDialog extends JDialog implements ActionListener
{
    private final PureFocus plugin_;
	private final PureFocusFrame parent_;
    private final Studio gui_;
	
	// GUI elements
    private JCheckBox isPiezoMotor_;
    private JCheckBox servoOn_;
    private JCheckBox servoInhibit_;
    private JCheckBox focusInterruptOn_;
    private JCheckBox interfaceInhibit_;
    private JTextField interfaceInhibitCount_;
    private JCheckBox digipotControlsOffset_;
    private JCheckBox isServoDirectionPositive_;
    private JCheckBox isFocusDriveDirectionPositive_;
    private JTextField exposureTimeUs_;
    private JTextField digipotOffsetSpeedPercent_;
    private JTextField focusDriveRangeMicrons_;
    private JTextField inFocusRecoveryTimeMs_;

	/** Creates new form PureFocusGlobalTableFrame
	@param parent Base window
    @param plugin PureFocus plugin
	@param gui MM scriptInterface
	*/
	public PureFocusGlobalTableDialog(PureFocusFrame parent, PureFocus plugin, Studio gui)
	{
        // We want this to be a modeless dialog, to work with the rest of the GUI
        super(parent, "Prior PureFocus PF-850 Global Settings");
            
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
        String pf = parent_.getPureFocus();
        CMMCore core = gui_.getCMMCore();

        try
		{
            isPiezoMotor_.setSelected(Long.valueOf(core.getProperty(pf, plugin_.IS_PIEZO_MOTOR)) != 0);
            servoOn_.setSelected(Long.valueOf(core.getProperty(pf, plugin_.SERVO_ON)) != 0);
            servoInhibit_.setSelected(Long.valueOf(core.getProperty(pf, plugin_.SERVO_INHIBIT)) != 0);
            focusInterruptOn_.setSelected(Long.valueOf(core.getProperty(pf, plugin_.FOCUS_INTERRUPT_ON)) != 0);
            interfaceInhibit_.setSelected(Long.valueOf(core.getProperty(pf, plugin_.INTERFACE_INHIBIT)) != 0);
            interfaceInhibitCount_.setText(core.getProperty(pf, plugin_.INTERFACE_INHIBIT_COUNT));
            digipotControlsOffset_.setSelected(Long.valueOf(core.getProperty(pf, plugin_.DIGIPOT_CONTROLS_OFFSET)) != 0);
            isServoDirectionPositive_.setSelected(Long.valueOf(core.getProperty(pf, plugin_.IS_SERVO_DIRECTION_POSITIVE)) != 0);
            isFocusDriveDirectionPositive_.setSelected(Long.valueOf(core.getProperty(pf, plugin_.IS_FOCUS_DRIVE_DIRECTION_POSITIVE)) != 0);
            exposureTimeUs_.setText(core.getProperty(pf, plugin_.EXPOSURE_TIME_US));
            digipotOffsetSpeedPercent_.setText(core.getProperty(pf, plugin_.DIGIPOT_OFFSET_SPEED_PERCENT));
            focusDriveRangeMicrons_.setText(core.getProperty(pf, plugin_.FOCUS_DRIVE_RANGE_MICRONS));
            inFocusRecoveryTimeMs_.setText(core.getProperty(pf, plugin_.IN_FOCUS_RECOVERY_TIME_MS));
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
        
        isPiezoMotor_ = new javax.swing.JCheckBox();
        isPiezoMotor_.addActionListener(this);   
        
        servoOn_ = new javax.swing.JCheckBox();
        servoOn_.addActionListener(this);   

        servoInhibit_ = new javax.swing.JCheckBox();
        servoInhibit_.addActionListener(this);   

        focusInterruptOn_ = new javax.swing.JCheckBox();
        focusInterruptOn_.addActionListener(this);   

        interfaceInhibit_ = new javax.swing.JCheckBox();
        interfaceInhibit_.addActionListener(this);   
        
        interfaceInhibitCount_ = new javax.swing.JTextField();
        interfaceInhibitCount_.setPreferredSize(new java.awt.Dimension(100, 20));
        interfaceInhibitCount_.addActionListener(this);    

        digipotControlsOffset_ = new javax.swing.JCheckBox();
        digipotControlsOffset_.addActionListener(this);   

        isServoDirectionPositive_ = new javax.swing.JCheckBox();
        isServoDirectionPositive_.addActionListener(this);   

        isFocusDriveDirectionPositive_ = new javax.swing.JCheckBox();
        isFocusDriveDirectionPositive_.addActionListener(this);          
        
        exposureTimeUs_ = new javax.swing.JTextField();
        exposureTimeUs_.setPreferredSize(new java.awt.Dimension(100, 20));
        exposureTimeUs_.addActionListener(this); 
        
        digipotOffsetSpeedPercent_ = new javax.swing.JTextField();
        digipotOffsetSpeedPercent_.setPreferredSize(new java.awt.Dimension(100, 20));
        digipotOffsetSpeedPercent_.addActionListener(this); 
        
        focusDriveRangeMicrons_ = new javax.swing.JTextField();
        focusDriveRangeMicrons_.setPreferredSize(new java.awt.Dimension(100, 20));
        focusDriveRangeMicrons_.addActionListener(this);
        
        inFocusRecoveryTimeMs_ = new javax.swing.JTextField();
        inFocusRecoveryTimeMs_.setPreferredSize(new java.awt.Dimension(100, 20));
        inFocusRecoveryTimeMs_.addActionListener(this); 
        
        // Add to layout
        this.add(new JLabel("Is piezo motor"), "align label");
        this.add(isPiezoMotor_, "wrap");
        
        this.add(new JLabel("Servo on"), "align label");
        this.add(servoOn_, "wrap");
        
        this.add(new JLabel("Servo inhibit"), "align label");
        this.add(servoInhibit_, "wrap");
        
        this.add(new JLabel("Focus interrupt on"), "align label");
        this.add(focusInterruptOn_, "wrap");
        
        this.add(new JLabel("Interface inhibit"), "align label");
        this.add(interfaceInhibit_, "wrap");
        
        this.add(new JLabel("Interface inhibit count"), "align label");
        this.add(interfaceInhibitCount_, "wrap");
        
        this.add(new JLabel("Digipot controls offset"), "align label");
        this.add(digipotControlsOffset_, "wrap");
        
        this.add(new JLabel("Is servo direction positive"), "align label");
        this.add(isServoDirectionPositive_, "wrap");
        
        this.add(new JLabel("Is focus drive direction positive"), "align label");
        this.add(isFocusDriveDirectionPositive_, "wrap");
        
        this.add(new JLabel("Exposure time (us)"), "align label");
        this.add(exposureTimeUs_, "wrap");
        
        this.add(new JLabel("Digipot offset speed (%)"), "align label");
        this.add(digipotOffsetSpeedPercent_, "wrap");
        
        this.add(new JLabel("Focus drive range (um)"), "align label");
        this.add(focusDriveRangeMicrons_, "wrap");
        
        this.add(new JLabel("In focus recovery time (ms)"), "align label");
        this.add(inFocusRecoveryTimeMs_, "wrap");

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

                if (source == interfaceInhibitCount_)
                {
                    String val = widget.getText();
                    core.setProperty(pf, plugin_.INTERFACE_INHIBIT_COUNT, val);
                }

                if (source == exposureTimeUs_)
                {
                    String val = widget.getText();
                    core.setProperty(pf, plugin_.EXPOSURE_TIME_US, val);
                }

                if (source == digipotOffsetSpeedPercent_)
                {
                    String val = widget.getText();
                    core.setProperty(pf, plugin_.DIGIPOT_OFFSET_SPEED_PERCENT, val);
                }

                if (source == focusDriveRangeMicrons_)
                {
                    String val = widget.getText();
                    core.setProperty(pf, plugin_.FOCUS_DRIVE_RANGE_MICRONS, val);
                }

                if (source == inFocusRecoveryTimeMs_)
                {
                    String val = widget.getText();
                    core.setProperty(pf, plugin_.IN_FOCUS_RECOVERY_TIME_MS, val);
                }                        
            }
            else if (source.getClass() == JCheckBox.class)
            {
                JCheckBox widget = (JCheckBox)source;
    
                if (source == isPiezoMotor_)
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
                    core.setProperty(pf, plugin_.IS_PIEZO_MOTOR, value);
                }
                
                if (source == servoOn_)
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
                    core.setProperty(pf, plugin_.SERVO_ON, value);
                }
                
                if (source == servoInhibit_)
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
                    core.setProperty(pf, plugin_.SERVO_INHIBIT, value);
                }
                
                if (source == focusInterruptOn_)
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
                    core.setProperty(pf, plugin_.FOCUS_INTERRUPT_ON, value);
                }
                
                if (source == interfaceInhibit_)
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
                    core.setProperty(pf, plugin_.INTERFACE_INHIBIT, value);
                }
                
                if (source == digipotControlsOffset_)
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
                    core.setProperty(pf, plugin_.DIGIPOT_CONTROLS_OFFSET, value);
                }
                
                if (source == isServoDirectionPositive_)
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
                    core.setProperty(pf, plugin_.IS_SERVO_DIRECTION_POSITIVE, value);
                }
                
                if (source == isFocusDriveDirectionPositive_)
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
                    core.setProperty(pf, plugin_.IS_FOCUS_DRIVE_DIRECTION_POSITIVE, value);
                }           
            }
            else
            {
                // Unknown so ignore it
            }
                
            core.setProperty(plugin_.DEVICE_NAME, plugin_.SINGLE_CHANGE_IN_PROGRESS, 0);
            
            // Update current settings
            updateValues(true);
            
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
                updateValues(true);
            }
            catch (Exception e2)
            {
                // These actions should not be able to fail
            }
        }
	}
}
