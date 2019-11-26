package com.spiderclould.resolver;

import org.springframework.stereotype.Service;

import com.spiderclould.entity.AccessToken;

@Service
public class TokenStorageResolver {

    private AccessToken accessToken;

    public AccessToken getToken() {
        return this.getAccessToken();
    }

    public void saveToken(AccessToken accessToken) {
        this.setAccessToken(accessToken);
    }

    public TokenStorageResolver setAccessToken(AccessToken token){
        this.accessToken = token;
        return this;
    }

    public AccessToken getAccessToken(){
        return this.accessToken;
    }

}
