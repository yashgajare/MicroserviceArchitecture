package com.microservices.user.service;

import com.microservices.user.entity.User;
import com.microservices.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import javax.cache.Cache;
import javax.cache.annotation.CachePut;

@Service
public class UserService {
    
	
	@Autowired
	private javax.cache.CacheManager cacheManager;
	
    @Autowired
    private UserRepository userRepository;
    
    @Cacheable(value = "users", key = "T(String).valueOf(#id)")
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
    
    @Cacheable(value = "users", key = "#email")
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
//    @CachePut(value = "users", key = "#result.id")
    public User createUser(User user) {
        return userRepository.save(user);
    }
    
//    @CachePut(value = "users", key = "#result.id")
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.setName(userDetails.getName());
            user.setEmail(userDetails.getEmail());
            return userRepository.save(user);
        }
        return null;
    }
    
    @CacheEvict(value = "users", key = "#id")
    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
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