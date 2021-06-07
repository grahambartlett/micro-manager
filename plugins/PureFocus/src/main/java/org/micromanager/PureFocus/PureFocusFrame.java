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
public class PureFocusFrame extends JFrame
{
	private final Studio gui_;
	private final CMMCore core_;
	private final PureFocus plugin_;
	
	
	// GUI elements
	private JSpinner objectiveSelectSpinner_;
    
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

		// If we get here, we know we have a PureFocus connected
        initComponents();

        super.setIconImage(Toolkit.getDefaultToolkit().getImage(
                    getClass().getResource("/org/micromanager/icons/microscope.gif")));

        updateValues(true);

        objectiveSlotTableDialog_ = new PureFocusObjectiveSlotTableDialog(this, plugin_, gui_);
        objectiveSlotTableDialog_.setVisible(true);
        
        globalTableDialog_ = new PureFocusGlobalTableDialog(this, plugin_, gui_);
        globalTableDialog_.setVisible(true);        
	}


    private void updateValues(boolean allValues)
	{
		try
		{
			String val;
			if (allValues)
			{
			}
 
			val = core_.getProperty(plugin_.DEVICE_NAME, "Objective");
            Integer intVal = Integer.parseInt(val);
            SpinnerModel numberModel = objectiveSelectSpinner_.getModel();
            numberModel.setValue(intVal);
		}
		catch (Exception ex)
		{
			gui_.logs().showError("Error reading values from Prior PureFocus PF-850");
		}
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
        
        this.setLayout(new MigLayout("", "[100]30[200]"));

		objectiveSelectSpinner_ = new javax.swing.JSpinner();
		objectiveSelectSpinner_.setModel(new javax.swing.SpinnerNumberModel(1, 1, 6, 1));
		objectiveSelectSpinner_.setPreferredSize(new java.awt.Dimension(100, 20));
		objectiveSelectSpinner_.addChangeListener(new javax.swing.event.ChangeListener()
		{
			@Override
			public void stateChanged(javax.swing.event.ChangeEvent evt)
			{
				objectiveSelectSpinner_StateChanged(evt);
			}
		});
		this.add(objectiveSelectSpinner_);

		pack();
	}

				
	private void objectiveSelectSpinner_StateChanged(javax.swing.event.ChangeEvent evt)
	{
		SpinnerModel numberModel = objectiveSelectSpinner_.getModel();

		int newValue = (Integer)numberModel.getValue();
		try
		{
			core_.setProperty(plugin_.DEVICE_NAME, "Objective", newValue);
		}
		catch (Exception ex)
		{
			gui_.logs().showError("Problem while setting Prior PureFocus PF-850 objective");
		}
	}

    /** @return Name of currently-selected PureFocus */
    public String getPureFocus()
    {
        return plugin_.DEVICE_NAME;
    }
}
