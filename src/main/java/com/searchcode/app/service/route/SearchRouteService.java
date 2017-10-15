/*
 * Copyright (c) 2016 Boyter Online Services
 *
 * Use of this software is governed by the Fair Source License included
 * in the LICENSE.TXT file, but will be eventually open under GNU General Public License Version 3
 * see the README.md for when this clause will take effect
 *
 * Version 1.3.12
 */

package com.searchcode.app.service.route;

import com.searchcode.app.config.Values;
import com.searchcode.app.dto.SearchResult;
import com.searchcode.app.service.CodeMatcher;
import com.searchcode.app.service.Singleton;
import com.searchcode.app.util.SearchcodeLib;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.QueryParser;
import spark.Request;
import spark.Response;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class SearchRouteService {

    public SearchResult codeSearch(Request request, Response response) {
        return this.getSearchResult(request, false);
    }

    public SearchResult literalCodeSearch(Request request, Response response) {
        return this.getSearchResult(request, true);
    }

    private SearchResult getSearchResult(Request request, boolean isLiteral) {
        if (!request.queryParams().contains("q") || request.queryParams("q").trim().equals(Values.EMPTYSTRING)) {
            return null;
        }

        String query = request.queryParams("q").trim();
        CodeMatcher cm = new CodeMatcher();
        SearchcodeLib scl = Singleton.getSearchcodeLib();

        int page = 0;

        if (request.queryParams().contains("p")) {
            try {
                page = Integer.parseInt(request.queryParams("p"));
                page = page > 19 ? 19 : page;
            }
            catch (NumberFormatException ex) {
                page = 0;
            }
        }

        HashMap<String, String[]> facets = new HashMap<>();

        if (request.queryParams().contains("repo")) { facets.put("repo", request.queryParamsValues("repo")); }
        if (request.queryParams().contains("lan")) { facets.put("lan", request.queryParamsValues("lan")); }
        if (request.queryParams().contains("own")) { facets.put("own", request.queryParamsValues("own")); }
        if (request.queryParams().contains("fl")) { facets.put("fl", new String[] { request.queryParams("fl") }); }

        if (query.trim().startsWith("/") && query.trim().endsWith("/")) { isLiteral = true; }

        SearchResult searchResult = Singleton.getIndexService().search(query, facets, page, isLiteral);

        searchResult.setCodeResultList(cm.formatResults(searchResult.getCodeResultList(), query, true));
        searchResult.setQuery(query);

        for (String altQuery: scl.generateAltQueries(query)) {
            searchResult.addAltQuery(altQuery);
        }

        // Null out code as it isnt required and there is no point in bloating our ajax requests
        searchResult.getCodeResultList().forEach(x -> x.setCode(null));

        return searchResult;
    }
}
