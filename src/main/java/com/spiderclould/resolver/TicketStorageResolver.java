package com.spiderclould.resolver;

import org.springframework.stereotype.Service;

import com.spiderclould.entity.Ticket;
import com.spiderclould.entity.TicketStore;

@Service
public class TicketStorageResolver {

    private TicketStore ticketStore;

    public TicketStorageResolver(){
        this.ticketStore = new TicketStore();
    }

    public TicketStorageResolver(TicketStore store){
        this.ticketStore = store;
    }

    public Ticket getTicket(String type) {
        return this.getTicketStore().get(type);
    }

    public void saveTicket(String type, Ticket ticket) {
        this.getTicketStore().put(type, ticket);
    }

    public TicketStorageResolver setTicketStore(TicketStore store){
        this.ticketStore = store;
        return this;
    }

    public TicketStore getTicketStore(){
        return this.ticketStore;
    }

}
