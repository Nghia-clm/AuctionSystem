package com.auction.model;

public enum AuctionStatus {
    OPEN,      // mới tạo, chưa bắt đầu
    RUNNING,   // đang diễn ra
    FINISHED,  // đã kết thúc, có người thắng
    PAID,      // người thắng đã thanh toán
    CANCELED   // bị hủy
}
