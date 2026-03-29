CREATE INDEX IF NOT EXISTS idx_laptop_product_code
    ON laptop (product_code);

CREATE INDEX IF NOT EXISTS idx_laptop_detail_page
    ON laptop (detail_page);

CREATE INDEX IF NOT EXISTS idx_laptop_recommend_price_weight
    ON laptop (price, weight);

CREATE INDEX IF NOT EXISTS idx_laptop_recommend_price_screen
    ON laptop (price, screen_size);

CREATE INDEX IF NOT EXISTS idx_laptop_profile_office_score
    ON laptop_profile (office_score);

CREATE INDEX IF NOT EXISTS idx_laptop_profile_battery_score
    ON laptop_profile (battery_score);

CREATE INDEX IF NOT EXISTS idx_laptop_profile_casual_game_score
    ON laptop_profile (casual_game_score);

CREATE INDEX IF NOT EXISTS idx_laptop_profile_online_game_score
    ON laptop_profile (online_game_score);

CREATE INDEX IF NOT EXISTS idx_laptop_profile_aaa_game_score
    ON laptop_profile (aaa_game_score);

CREATE INDEX IF NOT EXISTS idx_laptop_profile_creator_score
    ON laptop_profile (creator_score);
