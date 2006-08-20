/*##########################################################################
 _##
 _##  $Id$
 _##
 _##########################################################################*/

package jrds;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * A class to find probe by their names
 * @author Fabrice Bacchella 
 * @version $Revision$,  $Date$
 */
public class ProbeFactory {

	private final Logger logger = Logger.getLogger(ProbeFactory.class);
	final private List<String> argPackages = new ArrayList<String>(3);
	final private List<String> probePackages = new ArrayList<String>(5);
	private Map<String, ProbeDesc> probeDescMap;
	private GraphFactory gf;
	private Properties prop;
	/**
	 * Private constructor
	 */
	ProbeFactory(Map<String, ProbeDesc> probeDescMap, GraphFactory gf, Properties prop) {
		this.probeDescMap = probeDescMap;
		this.gf = gf;
		this.prop = prop;
		argPackages.add("java.lang.");
		argPackages.add("java.net.");
		argPackages.add("");

		probePackages.add("jrds.probe.");
		probePackages.add("jrds.probe.snmp.");
		probePackages.add("jrds.probe.munins.");
		probePackages.add("jrds.probe.rstat.");
		probePackages.add("jrds.probe.jdbc.");
		probePackages.add("");
	}

	/**
	 * Create an objet providing the class name and a String argument. So the class must have
	 * a constructor taking only a string as an argument.
	 * @param className
	 * @param value
	 * @return
	 */
	public Object makeArg(String className, String value) {
		Object retValue = null;
		Class classType = resolvClass(className, argPackages);
		if (classType != null) {
			Class[] argsType = { String.class };
			Object[] args = { value };

			try {
				Constructor theConst = classType.getConstructor(argsType);
				retValue = theConst.newInstance(args);
			}
			catch (Exception ex) {
				logger.warn("Error during of creation :" + className + ": ", ex);
			}
		}
		return retValue;
	}

	/**
	 * Create an probe, provided his Class and a list of argument for a constructor
	 * for this object. It will be found using the default list of possible package
	 * @param className the probe name
	 * @param constArgs
	 * @return
	 */
	public Probe makeProbe(String className, List constArgs) {
		Probe retValue = null;
		ProbeDesc pd = (ProbeDesc) probeDescMap.get(className);
		if( pd != null) {
			retValue = pd.makeProbe(constArgs, prop);
		}
		else {
			Class probeClass = resolvClass(className, probePackages);
			if (probeClass != null) {
				Object o = null;
				try {
					Class[] constArgsType = new Class[constArgs.size()];
					Object[] constArgsVal = new Object[constArgs.size()];
					int index = 0;
					for (Object arg: constArgs) {
						constArgsType[index] = arg.getClass();
						constArgsVal[index] = arg;
						index++;
					}
					Constructor theConst = probeClass.getConstructor(constArgsType);
					o = theConst.newInstance(constArgsVal);
					retValue = (Probe) o;
				}
				catch (ClassCastException ex) {
					logger.warn("didn't get a Probe but a " + o.getClass().getName());
				}
				catch (Exception ex) {
					logger.warn("Error during probe creation of type " + className +
							": " + ex, ex);
				}
			}
		}
		
		//Now we finish the initialization of classes
		if(retValue != null) {
			retValue.readProperties(prop);
			retValue.initGraphList(gf);
		}
		return retValue;
	}

	private Class<?> resolvClass(String name, List<String> listPackages) {
		Class retValue = null;
		for (String packageTry: listPackages) {
			try {
				retValue = Class.forName(packageTry + name);
			}
			catch (ClassNotFoundException ex) {
			}
			catch (NoClassDefFoundError ex) {
			}
		}
		if (retValue == null)
			logger.warn("Class " + name + " not found");
		return retValue;
	}

	public ProbeDesc getProbeDesc(String name) {
		return probeDescMap.get(name);
	}
}
