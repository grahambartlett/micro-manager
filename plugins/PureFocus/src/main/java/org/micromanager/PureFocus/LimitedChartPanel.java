/**
@file LimitedChartPanel.java
@author Graham Bartlett
@copyright Prior Scientific Instruments Limited, 2021
@brief Micro-Manager plugin for control of the Prior PureFocus PF-850 autofocus unit

Micro-Manager plugin giving GUI control of the PF-850 setup and configuration.

Licensed under the BSD license.
*/

package org.micromanager.PureFocus;

import java.awt.Color;
import java.awt.Paint;
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
import javax.swing.JPopupMenu;
import javax.swing.JMenu;
import javax.swing.MenuElement;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;

import java.text.DecimalFormat;

import mmcorej.CMMCore;
import mmcorej.DeviceType;
import net.miginfocom.swing.MigLayout;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.RangeType;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.axis.ValueAxis;

import org.micromanager.Studio;
import org.micromanager.internal.utils.WindowPositioning;


/** Customised chart panel which prevents zoom/autorange outside 
 * fixed limits.
 */
@SuppressWarnings(value = {"serial", "static-access"})  
public class LimitedChartPanel extends ChartPanel
{
    protected double minX_, maxX_, minY_, maxY_;
    
    public LimitedChartPanel(JFreeChart graph, double minX, double maxX, double xTickInterval, double minY, double maxY, double yTickInterval, Paint colour)
    {
        /* Allow save, print, zoom.  Do not show properties, which would let the
        user change settings and break this.  We also do not need tooltips.
        */
        super(graph, false, true, true, true, false);
        
        minX_ = minX;
        maxX_ = maxX;
        minY_ = minY;
        maxY_ = maxY;
        
        Plot plot = graph.getPlot();
        if (plot.getClass() == XYPlot.class)
        {
            XYPlot xyPlot = (XYPlot)plot;        
            NumberAxis domainAxis = (NumberAxis)xyPlot.getDomainAxis();
            NumberAxis rangeAxis = (NumberAxis)xyPlot.getRangeAxis(); 

            /* We never want margins */
            domainAxis.setLowerMargin(0.0);
            domainAxis.setUpperMargin(0.0);
            rangeAxis.setLowerMargin(0.0);
            rangeAxis.setUpperMargin(0.0);            
            
            /* Initial view should be at limits  */
            domainAxis.setRange(minX, maxX);
            rangeAxis.setRange(minY, maxY);        
            
            /* Set up graph lines */
            domainAxis.setTickUnit(new NumberTickUnit(100.0, new DecimalFormat(), 1));
            domainAxis.setVerticalTickLabels(false);

            rangeAxis.setTickUnit(new NumberTickUnit(yTickInterval, new DecimalFormat(), 1));
            rangeAxis.setVerticalTickLabels(false);            
    
            /* Show crosshairs for locating points */
            xyPlot.setDomainCrosshairVisible(true);
            xyPlot.setRangeCrosshairVisible(true);
            
            /* Set colour */
            XYItemRenderer renderer = xyPlot.getRenderer();
            renderer.setSeriesPaint(0, colour);
        }
        
        /* No legend below graph */
        graph.removeLegend();
    }
    
    
    @Override
    public void restoreAutoDomainBounds()
    {
        super.restoreAutoDomainBounds();
        forceLimitedZoom();
    }

    
    @Override
    public void restoreAutoRangeBounds()
    {
        super.restoreAutoRangeBounds();
        forceLimitedZoom();
    }
    
    
    @Override
    public void restoreAutoBounds()
    {
        super.restoreAutoBounds();
        forceLimitedZoom();
    }
    
    
    @Override
    public void	zoom​(Rectangle2D selection)
    {
        super.zoom​(selection);
        forceLimitedZoom();
    }
    
    
    @Override
    public void zoomInBoth​(double x, double y)
    {
        super.zoomInBoth​(x, y);
        forceLimitedZoom();
    }
    

    @Override
    public void zoomInDomain​(double x, double y)
    {
        super.zoomInDomain​(x, y);
        forceLimitedZoom();
    }
    
    
    @Override
    public void zoomInRange​(double x, double y)
    {
        super.zoomInRange​(x ,y);
        forceLimitedZoom();
    }
    
    
    @Override
    public void zoomOutBoth​(double x, double y)	
    {
        super.zoomOutBoth​(x, y);
        forceLimitedZoom();
    }
    
    
    @Override
    public void zoomOutDomain​(double x, double y)
    {
        super.zoomOutDomain​(x, y);
        forceLimitedZoom();
    }
    
    
    @Override
    public void zoomOutRange​(double x, double y)
    {
        super.zoomOutRange​(x, y);
        forceLimitedZoom();
    }
    
    
    public void forceLimitedZoom()
    {
        JFreeChart chart = getChart();
        Plot plot = chart.getPlot();
        
        if (plot.getClass() == XYPlot.class)
        {
            XYPlot xyPlot = (XYPlot)plot;
            
            ValueAxis domainAxis = xyPlot.getDomainAxis();
            ValueAxis rangeAxis = xyPlot.getRangeAxis();
            
            double minX, maxX, minY, maxY;
            boolean xLimit, yLimit;

            xLimit = false;
            yLimit = false;
            
            minX = domainAxis.getLowerBound();
            maxX = domainAxis.getUpperBound();
            minY = rangeAxis.getLowerBound();
            maxY = rangeAxis.getUpperBound();

            if (minX < minX_)
            {
                minX = minX_;
                xLimit = true;
            }

            if (maxX > maxX_)
            {
                maxX = maxX_;
                xLimit = true;
            }
             
            if (minY < minY_)
            {
                minY = minY_;
                yLimit = true;
            }

            if (maxY > maxY_)
            {
                maxY = maxY_;
                yLimit = true;
            }      
            
            if (xLimit)
            {
                domainAxis.setRange(minX, maxX);
            }

            if (yLimit)
            {
                rangeAxis.setRange(minY, maxY);
            }
        }
    }
}