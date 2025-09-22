package pe.edu.vallegrande.ms_pagos.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import pe.edu.vallegrande.ms_pagos.model.Pago;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class PagoRepository {
    
    private final Map<String, Pago> pagos = new ConcurrentHashMap<>();
    
    /**
     * Guarda un pago en el repositorio en memoria
     */
    public Pago save(Pago pago) {
        log.debug("Guardando pago con ID: {}", pago.getId());
        pagos.put(pago.getId(), pago);
        return pago;
    }
    
    /**
     * Busca un pago por su ID
     */
    public Optional<Pago> findById(String id) {
        log.debug("Buscando pago con ID: {}", id);
        return Optional.ofNullable(pagos.get(id));
    }
    
    /**
     * Busca pagos por ID de orden
     */
    public List<Pago> findByOrderId(String orderId) {
        log.debug("Buscando pagos para la orden: {}", orderId);
        return pagos.values().stream()
                .filter(pago -> orderId.equals(pago.getOrderId()))
                .collect(Collectors.toList());
    }
    
    /**
     * Busca pagos por ID de cliente
     */
    public List<Pago> findByClienteId(String clienteId) {
        log.debug("Buscando pagos para el cliente: {}", clienteId);
        return pagos.values().stream()
                .filter(pago -> clienteId.equals(pago.getClienteId()))
                .collect(Collectors.toList());
    }
    
    /**
     * Busca pagos por estado
     */
    public List<Pago> findByEstado(Pago.EstadoPago estado) {
        log.debug("Buscando pagos con estado: {}", estado);
        return pagos.values().stream()
                .filter(pago -> estado.equals(pago.getEstado()))
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene todos los pagos
     */
    public List<Pago> findAll() {
        log.debug("Obteniendo todos los pagos. Total: {}", pagos.size());
        return new ArrayList<>(pagos.values());
    }
    
    /**
     * Elimina un pago por su ID
     */
    public boolean deleteById(String id) {
        log.debug("Eliminando pago con ID: {}", id);
        return pagos.remove(id) != null;
    }
    
    /**
     * Verifica si existe un pago con el ID dado
     */
    public boolean existsById(String id) {
        return pagos.containsKey(id);
    }
    
    /**
     * Cuenta el total de pagos
     */
    public long count() {
        return pagos.size();
    }
    
    /**
     * Limpia todos los pagos (útil para testing)
     */
    public void deleteAll() {
        log.debug("Eliminando todos los pagos");
        pagos.clear();
    }
}