from unittest.mock import patch, MagicMock
import app.services.rag_service as rag_service


def _make_mock_collection(docs, names, infos):
    """ChromaDB collection mock 생성 헬퍼."""
    col = MagicMock()
    col.count.return_value = len(docs)
    col.query.return_value = {
        "documents": [docs],
        "metadatas": [[{"name": n, "info": i} for n, i in zip(names, infos)]],
    }
    return col


def test_search_returns_list_of_dicts():
    mock_col = _make_mock_collection(
        docs=["닭가슴살 100g: 열량 109kcal, 단백질 23g..."],
        names=["닭가슴살"],
        infos=["100g당 단백질 23g, 열량 109kcal"],
    )
    with patch.object(rag_service, "_get_collection", return_value=mock_col):
        result = rag_service.search("단백질 많은 음식")

    assert isinstance(result, list)
    assert len(result) == 1
    assert result[0]["name"] == "닭가슴살"
    assert result[0]["info"] == "100g당 단백질 23g, 열량 109kcal"
    assert "document" in result[0]


def test_search_passes_query_to_collection():
    mock_col = _make_mock_collection(
        docs=["시금치 100g: 철분 2.7mg..."],
        names=["시금치"],
        infos=["100g당 철분 2.7mg, 열량 23kcal"],
    )
    with patch.object(rag_service, "_get_collection", return_value=mock_col):
        rag_service.search("철분 많은 음식", n_results=3)

    mock_col.query.assert_called_once()
    call_kwargs = mock_col.query.call_args
    assert "철분 많은 음식" in call_kwargs.kwargs.get("query_texts", call_kwargs.args[0] if call_kwargs.args else [])


def test_search_returns_empty_when_no_docs():
    mock_col = MagicMock()
    mock_col.count.return_value = 0
    mock_col.query.return_value = {"documents": [[]], "metadatas": [[]]}
    with patch.object(rag_service, "_get_collection", return_value=mock_col):
        result = rag_service.search("아무거나")

    assert result == []


def test_search_n_results_capped_by_collection_count():
    """n_results > 컬렉션 크기일 때 에러 없이 동작해야 한다."""
    mock_col = _make_mock_collection(
        docs=["두부 100g..."],
        names=["두부"],
        infos=["100g당 단백질 8g"],
    )
    mock_col.count.return_value = 1
    with patch.object(rag_service, "_get_collection", return_value=mock_col):
        result = rag_service.search("단백질", n_results=10)

    assert len(result) == 1
    # n_results=min(10, 1)=1 로 query가 호출됐는지 확인
    call_kwargs = mock_col.query.call_args
    passed_n = call_kwargs.kwargs.get("n_results") or call_kwargs.args[1]
    assert passed_n == 1
