-- Repair aggregate balances for commission rows created before pending balances were synchronized.
-- frozen_commission also contains withdrawals being reviewed, so both sources are included.
UPDATE dealer_user du
LEFT JOIN (
    SELECT shop_id,
           dealer_user_id,
           SUM(CASE WHEN status = 'pending' THEN commission_amount ELSE 0 END) AS pending_amount,
           SUM(CASE WHEN status = 'settled' THEN commission_amount ELSE 0 END) AS settled_amount
    FROM dealer_order
    WHERE is_delete = 0
    GROUP BY shop_id, dealer_user_id
) commission ON commission.shop_id = du.shop_id AND commission.dealer_user_id = du.id
LEFT JOIN (
    SELECT shop_id,
           dealer_user_id,
           SUM(CASE WHEN status IN ('applying', 'approved') THEN amount ELSE 0 END) AS frozen_withdraw_amount
    FROM dealer_withdraw
    WHERE is_delete = 0
    GROUP BY shop_id, dealer_user_id
) withdraws ON withdraws.shop_id = du.shop_id AND withdraws.dealer_user_id = du.id
SET du.total_commission = COALESCE(commission.settled_amount, 0),
    du.frozen_commission = COALESCE(commission.pending_amount, 0)
                         + COALESCE(withdraws.frozen_withdraw_amount, 0)
WHERE du.is_delete = 0;
