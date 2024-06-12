package lsi.ubu.servicios;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lsi.ubu.excepciones.AnulaBilleteTrenException;
import lsi.ubu.excepciones.CompraBilleteTrenException;
import lsi.ubu.util.PoolDeConexiones;

public class ServicioImpl implements Servicio {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServicioImpl.class);

	@Override
	public void anularBillete(Time hora, java.util.Date fecha, String origen, String destino, int nroPlazas, int ticket)
			throws SQLException {
		PoolDeConexiones pool = PoolDeConexiones.getInstance();
		
		Connection con = pool.getConnection();

		/* Conversiones de fechas y horas */
		java.sql.Date fechaSqlDate = new java.sql.Date(fecha.getTime());
		java.sql.Timestamp horaTimestamp = new java.sql.Timestamp(hora.getTime());
		
		ResultSet existeRecorrido = null;
		ResultSet cantidadTickets = null;

		try {
			
			PreparedStatement stBilletesLibres = con.prepareStatement(
					"SELECT viajes.idViaje FROM viajes INNER JOIN recorridos on viajes.idRecorrido = recorridos.idRecorrido WHERE viajes.fecha = ? and estacionOrigen = ? and estacionDestino = ?");

			stBilletesLibres.setDate(1, fechaSqlDate);
			stBilletesLibres.setString(2, origen);
			stBilletesLibres.setString(3, destino);

			existeRecorrido = stBilletesLibres.executeQuery();

			if (!existeRecorrido.next()) {
				throw new CompraBilleteTrenException(CompraBilleteTrenException.NO_EXISTE_VIAJE);
			}
			
			if (existeRecorrido.getInt(1) != ticket) {
				throw new AnulaBilleteTrenException(AnulaBilleteTrenException.NO_BILLETE);
			}
			
			PreparedStatement stExisteTicket = con.prepareStatement("SELECT cantidad FROM tickets WHERE idTicket = ?");

			stExisteTicket.setInt(1, ticket);

			cantidadTickets = stExisteTicket.executeQuery();
			
			cantidadTickets.next();
			
			int plazas_eliminar = cantidadTickets.getInt(1);
			if (plazas_eliminar != nroPlazas) {
				throw new AnulaBilleteTrenException(AnulaBilleteTrenException.PLAZAS_ERRONEAS);
			}
			
			String updPlazas = "UPDATE viajes v set v.nPlazasLibres = v.nPlazasLibres + ? WHERE v.fecha = ? AND v.idRecorrido IN (SELECT r.idRecorrido FROM recorridos r WHERE r.estacionOrigen = ? AND r.estacionDestino = ?)";
			PreparedStatement stUpdPlazas = con.prepareStatement(updPlazas);
			
			stUpdPlazas.setInt(1, nroPlazas);
			stUpdPlazas.setDate(2, fechaSqlDate);
			stUpdPlazas.setString(3, origen);
			stUpdPlazas.setString(4, destino);
			
			stUpdPlazas.executeUpdate();
			
			con.commit();

		} catch (SQLException e) {
			
			con.rollback();
			
			if (e instanceof CompraBilleteTrenException) {
				throw new CompraBilleteTrenException(e.getErrorCode());
			} else if (e instanceof AnulaBilleteTrenException) {
				throw new AnulaBilleteTrenException(e.getErrorCode());
			} else {
				LOGGER.info(e.getErrorCode() + ": " + e.getMessage());
			}

		} finally {
			if (existeRecorrido != null) {
				existeRecorrido.close();
			}
			if (cantidadTickets != null) {
				cantidadTickets.close();
			}
			
			con.close();
		}

	}

	@Override
	public void comprarBillete(Time hora, Date fecha, String origen, String destino, int nroPlazas)
			throws SQLException {
		PoolDeConexiones pool = PoolDeConexiones.getInstance();

		/* Conversiones de fechas y horas */
		java.sql.Date fechaSqlDate = new java.sql.Date(fecha.getTime());
		java.sql.Timestamp horaTimestamp = new java.sql.Timestamp(hora.getTime());

		Connection con = pool.getConnection();

		ResultSet existeRecorrido = null;
		ResultSet ticketsLibres = null;

		int ticketsDispo = 0;

		try {

			PreparedStatement stBilletesLibres = con.prepareStatement(
					"SELECT viajes.idViaje FROM viajes INNER JOIN recorridos on viajes.idRecorrido = recorridos.idRecorrido WHERE viajes.fecha = ? and estacionOrigen = ? and estacionDestino = ?");

			stBilletesLibres.setDate(1, fechaSqlDate);
			stBilletesLibres.setString(2, origen);
			stBilletesLibres.setString(3, destino);

			existeRecorrido = stBilletesLibres.executeQuery();
			if (!existeRecorrido.next()) {
				throw new CompraBilleteTrenException(CompraBilleteTrenException.NO_EXISTE_VIAJE);
			}
			int idViaje = existeRecorrido.getInt(1);

			String conPlazasLibres = "SELECT nPlazasLibres FROM viajes where idViaje = ?";
			PreparedStatement stTicketsLibres = con.prepareStatement(conPlazasLibres);

			stTicketsLibres.setInt(1, idViaje);

			ticketsLibres = stTicketsLibres.executeQuery();
			if (ticketsLibres.next()) {
				ticketsDispo = ticketsLibres.getInt(1);
			}

			if (ticketsDispo < nroPlazas) {
				throw new CompraBilleteTrenException(CompraBilleteTrenException.NO_PLAZAS);
			}

			PreparedStatement stBilletes = con.prepareStatement(
					"INSERT INTO tickets (idTicket, idViaje, fechaCompra, cantidad, precio) VALUES (seq_tickets.nextval, ?, ?, ?, 50)");

			Date fechaActual = new Date();
			Timestamp actualDia = new Timestamp(fechaActual.getTime());
			stBilletes.setInt(1, idViaje);
			stBilletes.setTimestamp(2, actualDia);
			stBilletes.setInt(3, nroPlazas);

			stBilletes.executeUpdate();

			int libresFinal = ticketsDispo - nroPlazas;

			String updCantidad = "UPDATE viajes SET nPlazasLibres = ? WHERE idViaje = ?";
			PreparedStatement stCantidad = con.prepareStatement(updCantidad);

			stCantidad.setInt(1, libresFinal);
			stCantidad.setInt(2, idViaje);

			stCantidad.executeUpdate();

			con.commit();
		} catch (SQLException e) {
			con.rollback();

			if (e instanceof CompraBilleteTrenException) {
				throw new CompraBilleteTrenException(e.getErrorCode());
			} else {
				LOGGER.info(e.getErrorCode() + ": " + e.getMessage());
			}
		} finally {
			if (existeRecorrido != null)
				existeRecorrido.close();
			if (ticketsLibres != null)
				ticketsLibres.close();
			con.close();
		}
	}

	@Override
	public void modificarBillete(int billeteId, int nuevoNroPlazas) throws SQLException {
		// TODO Auto-generated method stub

	}

}
