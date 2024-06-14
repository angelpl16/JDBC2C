package lsi.ubu.excepciones;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModificaBilleteTrenException extends SQLException{
	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LoggerFactory.getLogger(CompraBilleteTrenException.class);

	public static final int NO_BILLETE = 1;
	public static final int VALOR_NEGATIVO = 2;
	public static final int PLAZAS_SUPERADAS = 3;
	public static final int NO_VIAJE = 4;
	
	
	private int codigo; // = -1;
	private String mensaje; 
	
	public ModificaBilleteTrenException (int code) {
		codigo = code;
		mensaje = null;
		
		switch (code) {
		case NO_BILLETE:
			mensaje="El billete indicado no existe";
			break;
		case VALOR_NEGATIVO:
			mensaje="El numero de plazas indicado es negativo, no se puede procesar";
			break;
		case PLAZAS_SUPERADAS:
			mensaje="El autobus no tiene capacidad para el numero de plazas que se quiere aumentar";
			break;
		case NO_VIAJE:
			mensaje="Hay problemas con el viaje asociado al billete";
		}
		
		LOGGER.debug(mensaje);

		// Traza_de_pila
		for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
			LOGGER.debug(ste.toString());
		}
	}
	
	@Override
	public String getMessage() { // Redefinicion del metodo de la clase Exception
		return mensaje;
	}

	@Override
	public int getErrorCode() { // Redefinicion del metodo de la clase SQLException
		return codigo;
	}
	


}
