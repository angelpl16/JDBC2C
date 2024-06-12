package lsi.ubu.excepciones;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnulaBilleteTrenException extends SQLException{
	private static final long serialVersionUID = 1;

	private static final Logger LOGGER = LoggerFactory.getLogger(CompraBilleteTrenException.class);

	public static final int NO_BILLETE = 1;
	public static final int PLAZAS_ERRONEAS= 2;

	private int codigo; // = -1;
	private String mensaje; 
	
	public AnulaBilleteTrenException (int code) {
		codigo = code;
		mensaje = null;
		
		switch (code) {
		case NO_BILLETE:
			mensaje = "No existe el billete indicado";
			break;
		case PLAZAS_ERRONEAS:
			mensaje = "El billete tiene un numero de plazas reservadas distinto a las que se quiere eliminar";
			break;
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
