package de.metas.order.grossprofit;

import static org.adempiere.model.InterfaceWrapperHelper.loadByIds;

import java.util.Collection;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import de.metas.money.Currency;
import de.metas.money.CurrencyId;
import de.metas.money.CurrencyRepository;
import de.metas.money.Money;
import de.metas.order.OrderLineId;
import de.metas.order.grossprofit.model.I_C_OrderLine;
import lombok.NonNull;

/*
 * #%L
 * de.metas.business
 * %%
 * Copyright (C) 2018 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

@Repository
public class OrderLineWithGrossProfitPriceRepository
{
	private final CurrencyRepository currencyRepository;

	public OrderLineWithGrossProfitPriceRepository(@NonNull final CurrencyRepository currencyRepository)
	{
		this.currencyRepository = currencyRepository;
	}

	public Optional<Money> getProfitBasePrice(@NonNull final OrderLineId orderLineId)
	{
		return getProfitMinBasePrice(ImmutableList.of(orderLineId));
	}

	public Optional<Money> getProfitMinBasePrice(@NonNull final Collection<OrderLineId> orderLineIds)
	{
		if (orderLineIds.isEmpty())
		{
			return Optional.empty();
		}

		final ImmutableSet<Money> profitBasePrices = loadByIds(OrderLineId.toIntSet(orderLineIds), I_C_OrderLine.class)
				.stream()
				.map(this::getProfitBasePrice)
				.collect(ImmutableSet.toImmutableSet());
		if (profitBasePrices.isEmpty())
		{
			return Optional.empty();
		}
		else if (profitBasePrices.size() == 1)
		{
			return Optional.of(profitBasePrices.iterator().next());
		}
		else if (Money.isSameCurrency(profitBasePrices))
		{
			return Optional.empty();
		}
		else
		{
			return profitBasePrices.stream().reduce(Money::min);
		}
	}

	private Money getProfitBasePrice(final I_C_OrderLine orderLineRecord)
	{
		final Currency currency = currencyRepository.getById(CurrencyId.ofRepoId(orderLineRecord.getC_Currency_ID()));
		return Money.of(orderLineRecord.getPriceGrossProfit(), currency);
	}
}
