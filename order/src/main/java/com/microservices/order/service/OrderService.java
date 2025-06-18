package com.microservices.order.service;

import com.microservices.order.client.ProductServiceClient;
import com.microservices.order.client.UserServiceClient;
import com.microservices.order.entity.Order;
import com.microservices.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.cache.Cache;

@Service
public class OrderService {
    
	@Autowired
	private javax.cache.CacheManager cacheManager;
	
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    @Autowired
    private ProductServiceClient productServiceClient;
    
    @Cacheable(value = "orders", key = "#id")
    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }
    
    @Cacheable(value = "orders", key = "#userId")
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }
    
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
    
    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }
    
//    @CacheEvict(value = "orders", allEntries = true)
    public Order createOrder(Long userId, Long productId, Integer quantity) {
        // Validate user exists
        UserServiceClient.UserDto user = userServiceClient.getUserById(userId);
        if (user == null) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        
        // Validate product exists and has sufficient quantity
        ProductServiceClient.ProductDto product = productServiceClient.getProductById(productId);
        if (product == null) {
            throw new RuntimeException("Product not found with id: " + productId);
        }
        
        if (product.getQuantity() < quantity) {
            throw new RuntimeException("Insufficient product quantity available");
        }
        
        // Calculate total amount
        BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        
        // Create order
        Order order = new Order(userId, productId, quantity, totalAmount);
        return orderRepository.save(order);
    }
    
//    @CacheEvict(value = "orders", key = "#id")
    public Order updateOrderStatus(Long id, Order.OrderStatus status) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order != null) {
            order.setStatus(status);
            return orderRepository.save(order);
        }
        return null;
    }
    
    @CacheEvict(value = "orders", key = "#id")
    public boolean cancelOrder(Long id) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order != null && order.getStatus() == Order.OrderStatus.PENDING) {
            order.setStatus(Order.OrderStatus.CANCELLED);
            orderRepository.save(order);
            return true;
        }
        return false;
    }
    
	public List<String> getCacheNames(){
		Iterable<String> cacheNames = cacheManager.getCacheNames();
		List<String> cacheList = new ArrayList();
		cacheNames.forEach(cacheList::add);
		return cacheList;
	}
	
	public Object getCacheData(String cacheName, String key) {
		Cache<String, Object> cache = cacheManager.getCache(cacheName);
		if(cache!=null) {
			System.out.println("Data: " + cache.get(key));
			return cache.get(key);
		}
		return null;
	}
	
	public void removeKeyFromCache(String cacheName, String key) {
		Cache<String, Object> cache = cacheManager.getCache(cacheName);
		if(cache!=null) {
			System.out.println("Removed key: " + key);
			cache.remove(key);
		}
	}
}
