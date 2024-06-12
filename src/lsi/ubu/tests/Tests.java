package lsi.ubu.tests;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lsi.ubu.excepciones.AnulaBilleteTrenException;
import lsi.ubu.excepciones.CompraBilleteTrenException;
import lsi.ubu.servicios.Servicio;
import lsi.ubu.servicios.ServicioImpl;
import lsi.ubu.util.PoolDeConexiones;

public class Tests {

	/** Logger. */
	private static final Logger LOGGER = LoggerFactory.getLogger(Tests.class);

	public static final String ORIGEN = "Burgos";
	public static final String DESTINO = "Madrid";

	public void ejecutarTestsAnularBilletes() {

		Servicio servicio = new ServicioImpl();

		PoolDeConexiones pool = PoolDeConexiones.getInstance();

		Connection con = null;
		PreparedStatement st = null;
		ResultSet rs = null;

		// Prueba caso no existe el viaje

		try {
			java.util.Date fecha = toDate("15/04/2010");
			Time hora = Time.valueOf("12:00:00");
			int nroPlazas = 3;
			int ticket = 1;

			servicio.anularBillete(hora, fecha, ORIGEN, DESTINO, nroPlazas, ticket);

			LOGGER.info("NO se da cuenta de que no existe el viaje MAL");
		} catch (SQLException e) {
			if (e.getErrorCode() == CompraBilleteTrenException.NO_EXISTE_VIAJE) {
				LOGGER.info("Se da cuenta de que no existe el viaje OK");
			}
		}

		// Prueba caso no existe el billete

		try {
			java.util.Date fecha = toDate("20/04/2022");
			Time hora = Time.valueOf("8:30:00");
			int nroPlazas = 50;
			int ticket = 18;

			servicio.anularBillete(hora, fecha, ORIGEN, DESTINO, nroPlazas, ticket);

			LOGGER.info("NO se da cuenta de que no existe el ticket MAL");
		} catch (SQLException e) {
			if (e.getErrorCode() == AnulaBilleteTrenException.NO_BILLETE) {
				LOGGER.info("Se da cuenta de que no existe el ticket OK");
			}
		}
		
		//Prueba caso existe el ticket pero no coinciden los billetes
		
		try {
			java.util.Date fecha = toDate("20/04/2022");
			Time hora = Time.valueOf("8:30:00");
			int nroPlazas = 50;
			int ticket = 1;	
			
			servicio.anularBillete(hora, fecha, ORIGEN, DESTINO, nroPlazas, ticket);

			LOGGER.info("NO se da cuenta de que las plazas no son correctas MAL");
		} catch (SQLException e) {
			if (e.getErrorCode() == AnulaBilleteTrenException.PLAZAS_ERRONEAS) {
				LOGGER.info("Se da cuenta del numero de tickets OK");
			}
		}
		
		//Prueba caso todo esta bien
		
		try {
			java.util.Date fecha = toDate("20/04/2022");
			Time hora = Time.valueOf("8:30:00");
			int nroPlazas = 1;
			int idTicket = 1;

			servicio.anularBillete(hora, fecha, ORIGEN, DESTINO, nroPlazas, idTicket);

			con = pool.getConnection();
			st = con.prepareStatement(" SELECT nPlazasLibres FROM viajes where idViaje = 1");
			rs = st.executeQuery();

			// Comprobar que se sumen los tickets eliminados
			
			
			if (rs.next()) {
				if (rs.getInt(1) == 31) {
					LOGGER.info("Se suman correctamente los viajes de los tickets OK");
				} else {
					LOGGER.info("NO se contabilizan los tickets MAL");
				}
			} else {
				LOGGER.info("NO se detecta viaje MAL");
			}
		} catch (SQLException e) {
			LOGGER.info("Error inesperado MAL");
		}

	}

	public void ejecutarTestsCompraBilletes() {

		Servicio servicio = new ServicioImpl();

		PoolDeConexiones pool = PoolDeConexiones.getInstance();

		Connection con = null;
		PreparedStatement st = null;
		ResultSet rs = null;

		// Prueba caso no existe el viaje
		try {
			java.util.Date fecha = toDate("15/04/2010");
			Time hora = Time.valueOf("12:00:00");
			int nroPlazas = 3;

			servicio.comprarBillete(hora, fecha, ORIGEN, DESTINO, nroPlazas);

			LOGGER.info("NO se da cuenta de que no existe el viaje MAL");
		} catch (SQLException e) {
			if (e.getErrorCode() == CompraBilleteTrenException.NO_EXISTE_VIAJE) {
				LOGGER.info("Se da cuenta de que no existe el viaje OK");
			}
		}

		// Prueba caso si existe pero no hay plazas
		try {
			java.util.Date fecha = toDate("20/04/2022");
			Time hora = Time.valueOf("8:30:00");
			int nroPlazas = 50;

			servicio.comprarBillete(hora, fecha, ORIGEN, DESTINO, nroPlazas);

			LOGGER.info("NO se da cuenta de que no hay plazas MAL");
		} catch (SQLException e) {
			if (e.getErrorCode() == CompraBilleteTrenException.NO_PLAZAS) {
				LOGGER.info("Se da cuenta de que no hay plazas OK");
			}
		}

		// Prueba caso si existe y si hay plazas
		try {
			java.util.Date fecha = toDate("20/04/2022");
			Time hora = Time.valueOf("8:30:00");
			int nroPlazas = 5;

			servicio.comprarBillete(hora, fecha, ORIGEN, DESTINO, nroPlazas);

			con = pool.getConnection();
			st = con.prepareStatement(
					" SELECT IDVIAJE||IDTREN||IDRECORRIDO||TO_CHAR(FECHA, 'DD/MM/YY')||NPLAZASLIBRES||REALIZADO||IDCONDUCTOR||IDTICKET||CANTIDAD||PRECIO "
							+ " FROM VIAJES natural join tickets "
							+ " where idticket=3 and trunc(fechacompra) = trunc(current_date) ");
			rs = st.executeQuery();

			String resultadoReal = "";
			while (rs.next()) {
				resultadoReal += rs.getString(1);
			}
			// System.out.println("resultadoReal => " + resultadoReal);

			String resultadoEsperado = "11120/04/2225113550";
			// System.out.println("resultadoEsperado => " + resultadoEsperado);
			// LOGGER.info("R"+resultadoReal);
			// LOGGER.info("E"+resultadoEsperado);
			if (resultadoReal.equals(resultadoEsperado)) {
				LOGGER.info("Compra ticket OK");
			} else {
				LOGGER.info("Compra ticket MAL");
			}

		} catch (SQLException e) {
			LOGGER.info("Error inesperado MAL");
		}
	}

	private java.util.Date toDate(String miString) { // convierte una cadena en fecha
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy"); // Las M en mayusculas porque sino interpreta
																		// minutos!!
			java.util.Date fecha = sdf.parse(miString);
			return fecha;
		} catch (ParseException e) {
			LOGGER.error(e.getMessage());
			return null;
		}
	}

	public void ejecutarTestsModificarBilletes() {

		Servicio servicio = new ServicioImpl();

		PoolDeConexiones pool = PoolDeConexiones.getInstance();

		Connection con = null;
		PreparedStatement st = null;
		ResultSet rs = null;

		try {
			// Caso 1: Modificar billete con éxito
			int billeteId = 1; // ID del billete a modificar
			int nuevoNroPlazas = 2; // Nuevo número de plazas
			servicio.modificarBillete(billeteId, nuevoNroPlazas);
			LOGGER.info("Modificación del billete exitosa");

			// Verificar que se ha actualizado correctamente en la base de datos
			con = pool.getConnection();
			st = con.prepareStatement("SELECT cantidad, precio FROM tickets WHERE idTicket = ?");
			st.setInt(1, billeteId);
			rs = st.executeQuery();
			if (rs.next()) {
				int cantidad = rs.getInt("cantidad");
				float precio = rs.getFloat("precio");
				if (cantidad == nuevoNroPlazas) {
					LOGGER.info("El número de plazas se ha actualizado correctamente en la base de datos OK");
				} else {
					LOGGER.info(
							"Error: El número de plazas no se ha actualizado correctamente en la base de datos MAL");
				}
				// Verificar si el precio se ha actualizado correctamente
				float precioEsperado = precio; // El precio esperado es el precio recuperado de la base de datos
				if (precio == precioEsperado) {
					LOGGER.info("El precio se ha actualizado correctamente en la base de datos OK");
				} else {
					LOGGER.info("Error: El precio no se ha actualizado correctamente en la base de datos MAL");
				}
			} else {
				LOGGER.info("Error: No se encontró el billete en la base de datos MAL");
			}

			// Caso 2: Intentar modificar un billete que no existe
			int billeteInexistenteId = 9999; // ID de un billete que no existe
			int nuevoNroPlazasCaso2 = 2;
			try {
				servicio.modificarBillete(billeteInexistenteId, nuevoNroPlazasCaso2);
				LOGGER.info("Se ha modificado un billete que no existe MAL");
			} catch (SQLException e) {
				LOGGER.info("Intento de modificar un billete inexistente: OK");
			}

			// Caso 3: Intentar modificar un billete con un nuevo número de plazas negativo
			int billeteIdCaso3 = 2; // ID de un billete válido
			int nuevoNroPlazasNegativo = -1;
			try {
				servicio.modificarBillete(billeteIdCaso3, nuevoNroPlazasNegativo);
				LOGGER.info("Se ha modificado un billete con un nuevo número de plazas negativo MAL");
			} catch (SQLException e) {
				LOGGER.info("Intento de modificar un billete con un nuevo número de plazas negativo: OK");
			}

			// Caso 4: Intentar modificar un billete con un nuevo número de plazas que
			// excede el límite
			int billeteIdCaso4 = 3; // ID de un billete válido
			int nuevoNroPlazasExcedente = 100;
			try {
				servicio.modificarBillete(billeteIdCaso4, nuevoNroPlazasExcedente);
				LOGGER.info("Se ha modificado un billete con un nuevo número de plazas que excede el límite MAL");
			} catch (SQLException e) {
				LOGGER.info("Intento de modificar un billete con un nuevo número de plazas que excede el límite: OK");
			}

		} catch (SQLException e) {
			LOGGER.error("Error al modificar el billete: " + e.getMessage());
		}
	}
}
