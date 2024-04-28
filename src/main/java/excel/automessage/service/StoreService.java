package excel.automessage.service;

import excel.automessage.domain.Store;
import excel.automessage.dto.StoreDTO;
import excel.automessage.dto.StoreListDTO;
import excel.automessage.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;

    public void saveAll(StoreListDTO storeListDTO) {

        List<Store> stores = storeListDTO.getStores().stream()
                .map(StoreDTO.Save::toEntity)
                .collect(Collectors.toList());
        storeRepository.saveAll(stores);

    }
}
