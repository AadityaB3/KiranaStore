package com.kirana.controller;

import com.kirana.dto.OrderRequest;
import com.kirana.model.*;
import com.kirana.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private OTPVerificationRepository otpVerificationRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest orderRequest) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = userRepository.findByUsername(auth.getName()).orElseThrow();

            Address address = addressRepository.findById(orderRequest.getAddressId())
                    .orElseThrow(() -> new RuntimeException("Address not found"));

            BigDecimal totalAmount = BigDecimal.ZERO;
            List<OrderItem> orderItems = new ArrayList<>();

            Order order = Order.builder()
                    .user(user)
                    .addressText(address.getStreetAddress() + ", " + address.getCity() + ", " + address.getPincode())
                    .paymentMethod(orderRequest.getPaymentMethod())
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            for (var itemReq : orderRequest.getItems()) {
                Product product = productRepository.findById(itemReq.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found"));

                Inventory inventory = inventoryRepository.findById(product.getId())
                        .orElseThrow(() -> new RuntimeException("Product inventory records not found"));

                if (inventory.getStockQuantity() < itemReq.getQuantity()) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Error: Out of stock for " + product.getName()));
                }

                BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
                totalAmount = totalAmount.add(itemTotal);

                orderItems.add(OrderItem.builder()
                        .order(order)
                        .product(product)
                        .productName(product.getName())
                        .price(product.getPrice())
                        .quantity(itemReq.getQuantity())
                        .totalPrice(itemTotal)
                        .build());

                // Reduce inventory stock
                inventory.setStockQuantity(inventory.getStockQuantity() - itemReq.getQuantity());
                inventoryRepository.save(inventory);
            }

            order.setTotalAmount(totalAmount);
            order.setPayableAmount(totalAmount);
            order.setItems(orderItems);

            Order savedOrder = orderRepository.save(order);

            // Generate 4 digit OTP Verification code
            String randomOtp = String.format("%04d", new Random().nextInt(10000));
            OTPVerification otpVerification = OTPVerification.builder()
                    .orderId(savedOrder.getId())
                    .otpCode(randomOtp)
                    .isVerified(false)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build();
            otpVerificationRepository.save(otpVerification);

            return ResponseEntity.ok(Map.of(
                "message", "Order placed successfully!",
                "orderId", savedOrder.getId(),
                "otpCode", randomOtp
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getMyOrders() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();

        boolean isOwner = user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_OWNER"));
        boolean isRider = user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_DELIVERY"));

        if (isOwner) {
            return ResponseEntity.ok(orderRepository.findAll());
        } else if (isRider) {
            return ResponseEntity.ok(orderRepository.findByDeliveryPartnerIdOrderByCreatedAtDesc(user.getId()));
        } else {
            return ResponseEntity.ok(orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()));
        }
    }

    @GetMapping("/unassigned")
    @PreAuthorize("hasAnyRole('OWNER', 'DELIVERY')")
    public ResponseEntity<List<Order>> getUnassignedOrders() {
        return ResponseEntity.ok(orderRepository.findByDeliveryPartnerIdIsNullAndStatus("PREPARING"));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('OWNER', 'DELIVERY')")
    public ResponseEntity<?> assignDeliveryPartner(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        Long riderId = body.get("riderId");
        order.setDeliveryPartnerId(riderId);
        order.setStatus("OUT_FOR_DELIVERY");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        return ResponseEntity.ok(Map.of("message", "Delivery partner assigned successfully"));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'DELIVERY')")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        order.setStatus(body.get("status").toUpperCase());
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        return ResponseEntity.ok(Map.of("message", "Order status updated successfully"));
    }

    @PostMapping("/{id}/verify-otp")
    @PreAuthorize("hasRole('DELIVERY')")
    public ResponseEntity<?> verifyDeliveryOtp(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String inputOtp = body.get("otp");
        OTPVerification otpVerification = otpVerificationRepository.findByOrderId(id).orElse(null);
        if (otpVerification == null || !otpVerification.getOtpCode().equals(inputOtp)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid OTP code. Access Denied."));
        }

        otpVerification.setIsVerified(true);
        otpVerificationRepository.save(otpVerification);

        Order order = orderRepository.findById(id).orElseThrow();
        order.setStatus("DELIVERED");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        return ResponseEntity.ok(Map.of("message", "OTP verified successfully. Order completed."));
    }
}
