package com.akshara.api.bookimport.provider;

import com.akshara.api.bookimport.dto.BookImportSource;
import com.akshara.api.bookimport.dto.ExternalBookSearchPage;
import com.akshara.api.bookimport.dto.ExternalBookSearchQuery;

public interface BookMetadataProvider {
    BookImportSource source();

    ExternalBookSearchPage search(ExternalBookSearchQuery query);
}
