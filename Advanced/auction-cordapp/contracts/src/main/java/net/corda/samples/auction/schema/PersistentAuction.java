package net.corda.samples.auction.schema;

import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "AUCTION_DETAIL")
public class PersistentAuction extends PersistentState implements Serializable {

    @Column private final String auctionId;
    @Column private final String assetId;
    @Column private final BigDecimal basePrice;
    @Column private final String currency;
    @Column private final BigDecimal highestBid;
    @Column private final String highestBidder;
    @Column private final LocalDateTime bidEndTime;
    @Column private final BigDecimal winningBid;
    @Column private final Boolean active;
    @Column private final String auctioneer;
    @Column private final String winner;

    public PersistentAuction() {
        this.auctionId = null;
        this.assetId = null;
        this.basePrice = null;
        this.currency = null;
        this.highestBid = null;
        this.highestBidder = null;
        this.bidEndTime = null;
        this.winningBid = null;
        this.active = null;
        this.auctioneer = null;
        this.winner = null;
    }

    public PersistentAuction(String auctionId, String assetId, BigDecimal basePrice, String currency, BigDecimal highestBid,
                             String highestBidder, LocalDateTime bidEndTime, BigDecimal winningBid, Boolean active,
                             String auctioneer, String winner) {
        this.auctionId = auctionId;
        this.assetId = assetId;
        this.basePrice = basePrice;
        this.currency = currency;
        this.highestBid = highestBid;
        this.highestBidder = highestBidder;
        this.bidEndTime = bidEndTime;
        this.winningBid = winningBid;
        this.active = active;
        this.auctioneer = auctioneer;
        this.winner = winner;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getAssetId() {
        return assetId;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getHighestBid() {
        return highestBid;
    }

    public String getHighestBidder() {
        return highestBidder;
    }

    public LocalDateTime getBidEndTime() {
        return bidEndTime;
    }

    public BigDecimal getWinningBid() {
        return winningBid;
    }

    public Boolean getActive() {
        return active;
    }

    public String getAuctioneer() {
        return auctioneer;
    }

    public String getWinner() {
        return winner;
    }
}
