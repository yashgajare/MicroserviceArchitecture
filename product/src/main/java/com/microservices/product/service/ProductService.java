package com.microservices.product.service;

import com.microservices.product.entity.Product;
import com.microservices.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import javax.cache.Cache;

@Service
public class ProductService {
    
	@Autowired
	private javax.cache.CacheManager cacheManager;
	
    @Autowired
    private ProductRepository productRepository;
    
    @Cacheable(value = "products", key = "T(String).valueOf(#id)")
    public Product getProductById(Long id) {
        return productRepository.findById(id).orElse(null);
    }
    
    @Cacheable(value = "products", key = "#name")
    public List<Product> getProductsByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }
    
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
//    @CacheEvict(value = "products", allEntries = true)
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }
    
//    @CacheEvict(value = "products", key = "#id")
    public Product updateProduct(Long id, Product productDetails) {
        Product product = productRepository.findById(id).orElse(null);
        if (product != null) {
            product.setName(productDetails.getName());
            product.setDescription(productDetails.getDescription());
            product.setPrice(productDetails.getPrice());
            product.setQuantity(productDetails.getQuantity());
            return productRepository.save(product);
        }
        return null;
    }
    
    @CacheEvict(value = "products", key = "#id")
    public boolean deleteProduct(Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
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
	
	public List<String> getCacheNames(){
		Iterable<String> cacheNames = cacheManager.getCacheNames();
		List<String> cacheList = new ArrayList();
		cacheNames.forEach(cacheList::add);
		return cacheList;
	}
}