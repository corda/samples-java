package net.corda.samples.auction.client

class Forms {

    class BidForm {
        var auctionId: String? = null
        var amount: Int = 0
    }

    class SettlementForm {
        var auctionId: String? = null
        var amount: String? = null
    }

    class CreateAuctionForm {
        var basePrice: Int = 0
        var assetId: String? = null
        var deadline: String? = null
    }

    class IssueCashForm {
        var party: String? = null
        var amount: Int = 0
    }

    class AssetForm {
        var imageUrl: String? = null
        var title: String? = null
        var description: String? = null
    }

}
