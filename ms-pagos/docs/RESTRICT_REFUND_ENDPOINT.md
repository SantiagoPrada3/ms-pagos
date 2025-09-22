# 📚 DOCUMENTACIÓN DEL ENDPOINT RESTRICT-REFUND

## 🎯 **ENDPOINT IMPLEMENTADO**

### **POST /api/pagos/restrict-refund**

Procesa un refund restringido validando que el monto solicitado no exceda el límite máximo permitido.

---

## 🔧 **ESPECIFICACIÓN TÉCNICA**

### **Request**
```http
POST /api/pagos/restrict-refund
Content-Type: application/json

{
  "orderId": "ORD-001",
  "amount": 150.00,
  "maxRefundable": 500.00
}
```

### **Response Exitoso (200 OK)**
```json
{
  "success": true,
  "message": "Refund restringido procesado exitosamente. Monto: 150.00, Límite: 500.00",
  "data": {
    "id": "pago-refunded-123",
    "orderId": "ORD-001",
    "monto": 1000.00,
    "estado": "REFUNDED",
    "fechaCreacion": "2025-09-21T21:30:00",
    "fechaActualizacion": "2025-09-21T22:08:00",
    "metodoPago": "Tarjeta de Crédito",
    "moneda": "PEN",
    "descripcion": "Pago de prueba",
    "clienteId": "CLI-001",
    "transactionId": "TXN_12345678",
    "paymentGateway": "Visa",
    "codigoRespuesta": "RESTRICTED_REFUND_SUCCESS",
    "mensajeRespuesta": "Refund restringido procesado exitosamente. Monto: 150.00, Límite: 500.00, Restante: 350.00"
  },
  "timestamp": "2025-09-21T22:08:00.123456789"
}
```

### **Response Error - Límite Excedido (400 Bad Request)**
```json
{
  "success": false,
  "message": "El monto del refund (600.00) excede el límite máximo permitido (500.00)",
  "data": null,
  "timestamp": "2025-09-21T22:08:00.123456789",
  "errorCode": "VALIDATION_ERROR"
}
```

### **Response Error - Orden No Encontrada (404 Not Found)**
```json
{
  "success": false,
  "message": "No se encontraron pagos para la orden: ORD-INEXISTENTE",
  "data": null,
  "timestamp": "2025-09-21T22:08:00.123456789",
  "errorCode": "PAGO_NOT_FOUND"
}
```

---

## 🧪 **EJEMPLOS DE PRUEBAS CON cURL**

### **1. Caso Exitoso - Refund Válido**
```bash
curl -X POST http://localhost:8080/api/pagos/restrict-refund \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD-001",
    "amount": 150.00,
    "maxRefundable": 500.00
  }'
```

### **2. Caso Error - Monto Excede Límite**
```bash
curl -X POST http://localhost:8080/api/pagos/restrict-refund \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD-001",
    "amount": 600.00,
    "maxRefundable": 500.00
  }'
```

### **3. Caso Error - Orden Inexistente**
```bash
curl -X POST http://localhost:8080/api/pagos/restrict-refund \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD-INEXISTENTE",
    "amount": 100.00,
    "maxRefundable": 500.00
  }'
```

### **4. Caso Error - Campos Obligatorios Vacíos**
```bash
curl -X POST http://localhost:8080/api/pagos/restrict-refund \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "",
    "amount": null,
    "maxRefundable": 500.00
  }'
```

### **5. Caso Límite - Monto Exacto al Límite**
```bash
curl -X POST http://localhost:8080/api/pagos/restrict-refund \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD-001",
    "amount": 500.00,
    "maxRefundable": 500.00
  }'
```

---

## 📋 **COLECCIÓN POSTMAN**

### **Configuración de Entorno**
```json
{
  "name": "MS-Pagos Environment",
  "values": [
    {
      "key": "base_url",
      "value": "http://localhost:8080/api",
      "enabled": true
    },
    {
      "key": "test_order_id",
      "value": "ORD-POSTMAN-001",
      "enabled": true
    }
  ]
}
```

### **Request Postman - Restrict Refund**
```json
{
  "name": "Restrict Refund - Válido",
  "request": {
    "method": "POST",
    "header": [
      {
        "key": "Content-Type",
        "value": "application/json"
      },
      {
        "key": "User-Agent",
        "value": "Postman/Santiago-Test"
      }
    ],
    "body": {
      "mode": "raw",
      "raw": "{\n  \"orderId\": \"{{test_order_id}}\",\n  \"amount\": 150.00,\n  \"maxRefundable\": 500.00\n}"
    },
    "url": {
      "raw": "{{base_url}}/pagos/restrict-refund",
      "host": ["{{base_url}}"],
      "path": ["pagos", "restrict-refund"]
    }
  },
  "response": []
}
```

### **Tests Automatizados en Postman**
```javascript
// Test para verificar respuesta exitosa
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response has success true", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.success).to.eql(true);
});

pm.test("Response contains restrict refund message", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.message).to.include("Refund restringido procesado exitosamente");
});

pm.test("Payment state is REFUNDED", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.estado).to.eql("REFUNDED");
});

pm.test("Response time is less than 2000ms", function () {
    pm.expect(pm.response.responseTime).to.be.below(2000);
});
```

---

## ⚡ **FLUJO DE PRUEBAS COMPLETO**

### **Paso 1: Crear un Pago Inicial**
```bash
curl -X POST http://localhost:8080/api/pagos \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD-RESTRICT-TEST",
    "monto": 1000.00,
    "metodoPago": "Tarjeta de Crédito",
    "moneda": "PEN",
    "clienteId": "CLI-RESTRICT-TEST",
    "descripcion": "Pago para test de restrict refund"
  }'
```

### **Paso 2: Procesar Restrict Refund**
```bash
curl -X POST http://localhost:8080/api/pagos/restrict-refund \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD-RESTRICT-TEST",
    "amount": 300.00,
    "maxRefundable": 500.00
  }'
```

### **Paso 3: Verificar Estado del Pago**
```bash
curl -X GET "http://localhost:8080/api/pagos/orden/ORD-RESTRICT-TEST"
```

---

## 🔍 **VALIDACIONES IMPLEMENTADAS**

### **Validaciones de Request**
- ✅ **orderId**: No puede estar vacío o null
- ✅ **amount**: No puede ser null, debe ser > 0
- ✅ **maxRefundable**: No puede ser null, debe ser > 0

### **Validaciones de Negocio**
- ✅ **Límite de refund**: `amount <= maxRefundable`
- ✅ **Orden existente**: Debe existir al menos un pago para la orden
- ✅ **Pago completado**: Debe haber un pago en estado COMPLETED
- ✅ **Monto vs pago original**: `amount <= monto_pago_original`

### **Códigos de Respuesta**
- ✅ **200 OK**: Refund procesado exitosamente
- ✅ **400 Bad Request**: Validaciones fallidas o límite excedido
- ✅ **404 Not Found**: Orden o pago no encontrado
- ✅ **415 Unsupported Media Type**: Content-Type incorrecto
- ✅ **500 Internal Server Error**: Error interno del servidor

---

## 📊 **MÉTRICAS DE COBERTURA**

### **Pruebas Implementadas**
- ✅ **16 tests** para PagoServiceRestrictRefundTest
- ✅ **6 tests** para RestrictRefundControllerTest
- ✅ **Total**: 22 nuevos tests para el endpoint

### **Escenarios Cubiertos**
- ✅ Refund válido exitoso
- ✅ Monto excediendo límite
- ✅ Orden inexistente
- ✅ Sin pagos completados
- ✅ Monto mayor al pago original
- ✅ Validaciones de campos obligatorios
- ✅ Pruebas parametrizadas para casos límite
- ✅ Múltiples pagos completados
- ✅ Monto exacto al límite

---

## 🎯 **AUTOR IDENTIFICADO**

**Desarrollador**: Santiago  
**Fecha**: 21 de septiembre de 2025  
**Versión**: MS-Pagos v0.0.1-SNAPSHOT  
**Framework**: Spring Boot 3.5.6  
**Commit**: Extension restrict-refund endpoint implemented  

### **Características Implementadas**
- ✅ Método restrictRefund en PagoService con validaciones de límite
- ✅ Endpoint POST /pagos/restrict-refund con RefundRequest DTO
- ✅ Configuración JaCoCo existente reutilizada
- ✅ Suite completa de pruebas unitarias y de integración
- ✅ Documentación técnica completa con ejemplos de uso

---

*Documentación generada automáticamente el 21 de septiembre de 2025*  
*MS-Pagos - Microservicio de Gestión de Pagos*