package org._3rivers_ashtanga._2013.jaxb.jaxb_app;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

public class JAXBUtil {

	/**
	 * get all elements of specified type of of list of jaxbelements
	 * 
	 * @param class1
	 * @param els
	 * @return list of objects of specified type
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> getElements(
		final Class<T> class1,
		final List<JAXBElement<?>> els)
	{
		final List<T> result = new LinkedList<>();
		for (final JAXBElement<?> i : els) {
			if (class1.isAssignableFrom(
				i.getValue().getClass())) {
				result.add((T) (i.getValue()));
			}
		}
		return result;
	}

	/**
	 * 
	 * @param class1
	 * @param els
	 * @return first object with given class
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getElement(
		final Class<? extends T> class1,
		final List<JAXBElement<?>> els)
	{
		T result = null;
		for (final JAXBElement<?> i : els) {
			if (class1.isAssignableFrom(
				i.getValue().getClass())) {
				result = (T) (i.getValue());
				break;
			}
		}
		return result;
	}

	/**
	 * Value for specified Element name
	 * @param class1
	 * @param els
	 * @param elname
	 * @return value or null
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getElement(
		final Class<? extends T> class1,
		final List<JAXBElement<?>> els,
		QName elname)
	{
		T result = null;
		for (final JAXBElement<?> i : els) {
			if (class1.isAssignableFrom(
				i.getValue().getClass()) && i.getName().equals(elname)) {
				result = (T) (i.getValue());
				break;
			}
		}
		return result;
	}

	public static boolean getBoolean(
		final List<JAXBElement<?>> els,
		QName elname)
	{
		Boolean result = null;
		for (final JAXBElement<?> i : els) {
			if (Boolean.class.isAssignableFrom(
				i.getValue().getClass()) && i.getName().equals(elname)) {
				result = (Boolean) (i.getValue());
				break;
			}
		}
		return result != null && result;
	}

}
